// routes/usuarios.js
const express = require("express");
const router = express.Router();
const { connect, oidMaybe } = require("../lib/mongo");
const { usuarioSchemaCreate, usuarioSchemaPatch } = require("../lib/validate");
const { normalizeRole } = require("../lib/roles");

// -------------------------------
// GET /api/usuarios
// Soporta: ?search=&rol=&estado=&page=&limit=
// Devuelve lista resumida (nombre completo, rol string y rol_id)
// -------------------------------
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");

    const search = (req.query.search || "").trim();
    const rolFilter = (req.query.rol || "").trim();
    const estadoFilter = (req.query.estado || "").trim();

    let { page, limit } = req.query;
    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const $and = [];
    if (search) {
      $and.push({
        $or: [
          { nombres:   { $regex: search, $options: "i" } },
          { apellidos: { $regex: search, $options: "i" } },
          { correo:    { $regex: search, $options: "i" } },
          { rol:       { $regex: search, $options: "i" } },
        ],
      });
    }
    if (rolFilter)   $and.push({ rol: rolFilter });
    if (estadoFilter)$and.push({ estado: estadoFilter });

    const q = $and.length ? { $and } : {};

    const cursor = col.aggregate([
      { $match: q },
      { $project: {
          _id: { $toString: "$_id" },
          userId: 1,
          nombres: 1,
          apellidos: 1,
          nombre: { $concat: [
            { $ifNull: ["$nombres", ""] }, " ",
            { $ifNull: ["$apellidos", ""] }
          ]},
          correo: 1,
          estado: 1,
          rol: 1,
          rol_id: 1,
          especialidad: 1,
        }
      },
      { $sort: { apellidos: 1, nombres: 1 } },
      { $skip: (page - 1) * limit },
      { $limit: limit }
    ]);

    const [data, total] = await Promise.all([
      cursor.toArray(),
      col.countDocuments(q),
    ]);

    res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// -------------------------------
// GET /api/usuarios/:id
// -------------------------------
router.get("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const doc = await col.findOne({ _id: oid });
    if (!doc) return res.status(404).json({ ok: false, error: "Usuario no encontrado" });

    doc._id = doc._id.toString();
    return res.json({ ok: true, data: doc });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// -------------------------------
// POST /api/usuarios
// Acepta rol o rol_id (normalizeRole deja ambos consistentes).
// "especialidad" es OPCIONAL (y si no viene, se guarda como [])
// -------------------------------
router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");

    // valida y normaliza (estado en minúscula, correo en minúscula, etc.)
    const parsed = usuarioSchemaCreate.parse(req.body);

    // Normaliza rol/rol_id (acepta cualquiera de los dos y devuelve ambos)
    const withRole = await normalizeRole(db, { ...parsed });

    // Especialidad siempre array (OPCIONAL)
    const especialidad = Array.isArray(withRole.especialidad)
      ? withRole.especialidad
      : withRole.especialidad
      ? [withRole.especialidad]
      : [];

    const now = new Date();
    const doc = {
      ...withRole,
      especialidad,
      createdAt: now,
      updatedAt: now,
    };

    const r = await col.insertOne(doc);
    return res.status(201).json({ ok: true, id: r.insertedId.toString() });
  } catch (e) {
    // mensaje amigable si choca con índice único de correo u otro validador
    if (String(e.message).includes("duplicate key") && String(e.message).includes("correo")) {
      return res.status(400).json({ ok: false, error: "Correo ya registrado" });
    }
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// -------------------------------
// PATCH /api/usuarios/:id
// Permite actualizar parciales, incluido cambio de rol/rol_id.
// Si envían userId en el body, se usa ese filtro en lugar de :id.
// -------------------------------
router.patch("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const patch = usuarioSchemaPatch.parse(req.body);

    // Filtro: por userId (si viene) o por _id de la ruta
    const filter = patch.userId
      ? { userId: patch.userId }
      : (() => {
          const oid = oidMaybe(req.params.id);
          if (!oid) throw new Error("id inválido");
          return { _id: oid };
        })();

    // Normaliza rol/rol_id del patch (si vienen)
    const normPatch = await normalizeRole(db, { ...patch });

    const $set = { updatedAt: new Date() };

    // Campos simples
    for (const k of ["nombres","apellidos","estado","rol","rol_id","direccion","telefono"]) {
      if (normPatch[k] != null) $set[k] = normPatch[k];
    }

    // correo a minúscula si viene
    if (normPatch.correo != null) $set.correo = normPatch.correo.trim().toLowerCase();

    // especialidad siempre array si viene
    if (normPatch.especialidad != null) {
      $set.especialidad = Array.isArray(normPatch.especialidad)
        ? normPatch.especialidad
        : [normPatch.especialidad];
    }

    const fieldsToUpdate = Object.keys($set).filter(k => k !== "updatedAt");
    if (fieldsToUpdate.length === 0) {
      return res.status(400).json({ ok: false, error: "Nada para actualizar" });
    }

    const r = await col.updateOne(filter, { $set });
    if (r.matchedCount === 0) {
      return res.status(404).json({ ok: false, error: "Usuario no encontrado" });
    }
    return res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    // Duplicidad de correo u otros errores de validación
    if (String(e.message).includes("duplicate key") && String(e.message).includes("correo")) {
      return res.status(400).json({ ok: false, error: "Correo ya registrado" });
    }
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// -------------------------------
// DELETE /api/usuarios/:id
// Hard delete simple (no hay referencia fuerte por ahora).
// -------------------------------
router.delete("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const r = await col.deleteOne({ _id: oid });
    if (r.deletedCount === 0) {
      return res.status(404).json({ ok: false, error: "Usuario no encontrado" });
    }
    res.json({ ok: true, deleted: r.deletedCount });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
