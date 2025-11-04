const { connect, oidMaybe } = require("../lib/mongo");
const { usuarioSchemaCreate, usuarioSchemaPatch } = require("../lib/validate");
const { normalizeRole } = require("../lib/roles");

exports.listarUsuarios = async (req, res) => {
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
      {
        $project: {
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
};

exports.obtenerUsuario = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const doc = await col.findOne({ _id: oid });
    if (!doc) return res.status(404).json({ ok: false, error: "Usuario no encontrado" });

    doc._id = doc._id.toString();
    res.json({ ok: true, data: doc });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
};

exports.crearUsuario = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const parsed = usuarioSchemaCreate.parse(req.body);

    const withRole = await normalizeRole(db, { ...parsed });

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
    res.status(201).json({ ok: true, id: r.insertedId.toString() });
  } catch (e) {
    if (String(e.message).includes("duplicate key") && String(e.message).includes("correo")) {
      return res.status(400).json({ ok: false, error: "Correo ya registrado" });
    }
    res.status(400).json({ ok: false, error: e.message });
  }
};

exports.actualizarUsuario = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("usuarios");
    const patch = usuarioSchemaPatch.parse(req.body);

    const filter = patch.userId
      ? { userId: patch.userId }
      : (() => {
          const oid = oidMaybe(req.params.id);
          if (!oid) throw new Error("id inválido");
          return { _id: oid };
        })();

    const normPatch = await normalizeRole(db, { ...patch });

    const $set = { updatedAt: new Date() };

    for (const k of ["nombres","apellidos","estado","rol","rol_id","direccion","telefono"]) {
      if (normPatch[k] != null) $set[k] = normPatch[k];
    }

    if (normPatch.correo != null) $set.correo = normPatch.correo.trim().toLowerCase();

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
    res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    if (String(e.message).includes("duplicate key") && String(e.message).includes("correo")) {
      return res.status(400).json({ ok: false, error: "Correo ya registrado" });
    }
    res.status(400).json({ ok: false, error: e.message });
  }
};

exports.eliminarUsuario = async (req, res) => {
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
};
