// routes/procedimientos.js
const express = require("express");
const { z } = require("zod");
const { connect, oidMaybe } = require("../lib/mongo");

const router = express.Router();

// ============================
// Schemas (Zod)
// ============================
const ProcCreate = z.object({
  tipo_procedimiento: z.string().min(1).transform(s => s.trim()),
  costo: z.number().nonnegative(),
  activo: z.boolean().optional().default(true),
});

const ProcPatch = z.object({
  tipo_procedimiento: z.string().min(1).optional(),
  costo: z.number().nonnegative().optional(),
  activo: z.boolean().optional(),
}).refine(v => Object.keys(v).length > 0, { message: "Nada para actualizar" });

// ============================
// Helpers
// ============================
function parseBool(v) {
  if (v == null) return null;
  const s = String(v).trim().toLowerCase();
  if (["1", "true", "t", "yes", "y"].includes(s)) return true;
  if (["0", "false", "f", "no", "n"].includes(s)) return false;
  return null;
}

// ============================
// POST /api/procedimientos
// ============================
router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("procedimientos");
    const parsed = ProcCreate.parse(req.body);

    const now = new Date();
    const doc = {
      tipo_procedimiento: parsed.tipo_procedimiento,
      costo: parsed.costo,
      activo: parsed.activo ?? true,
      createdAt: now,
      updatedAt: now,
    };

    const r = await col.insertOne(doc);
    return res.status(201).json({ ok: true, id: r.insertedId.toString() });
  } catch (e) {
    if (e && e.code === 11000) {
      // índice único en tipo_procedimiento
      return res.status(409).json({ ok: false, error: "tipo_procedimiento ya existe" });
    }
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/procedimientos
// Filtros: ?q=&activo=1|0|true|false&page=&limit=
// ============================
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("procedimientos");

    const { q } = req.query;
    let { page, limit, activo } = req.query;

    const query = {};
    if (q) {
      const rx = new RegExp(String(q), "i");
      query.tipo_procedimiento = rx;
    }
    const act = parseBool(activo);
    if (act !== null) query.activo = act;

    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const cursor = col.find(query).sort({ tipo_procedimiento: 1 }).skip((page - 1) * limit).limit(limit);
    const [data, total] = await Promise.all([cursor.toArray(), col.countDocuments(query)]);

    return res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/procedimientos/:id
// ============================
router.get("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const db = await connect();
    const doc = await db.collection("procedimientos").findOne({ _id: oid });
    if (!doc) return res.status(404).json({ ok: false, error: "No encontrado" });

    return res.json({ ok: true, data: doc });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// PATCH /api/procedimientos/:id
// ============================
router.patch("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const patch = ProcPatch.parse(req.body);
    const db = await connect();
    const col = db.collection("procedimientos");

    const $set = { updatedAt: new Date() };
    if (patch.tipo_procedimiento != null) $set.tipo_procedimiento = patch.tipo_procedimiento.trim();
    if (patch.costo != null) $set.costo = patch.costo;
    if (patch.activo != null) $set.activo = patch.activo;

    const r = await col.updateOne({ _id: oid }, { $set });
    if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "No encontrado" });
    return res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    if (e && e.code === 11000) {
      return res.status(409).json({ ok: false, error: "tipo_procedimiento ya existe" });
    }
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// DELETE /api/procedimientos/:id
// Protegido: si está referenciado en citas.procedimientos.procedimiento_id
// (legacy), bloquea borrado salvo ?force=1
// ============================
router.delete("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const db = await connect();
    const force = parseBool(req.query.force);

    // Verifica referencias (legacy)
    const refs = await db.collection("citas").countDocuments({ "procedimientos.procedimiento_id": oid });
    if (refs > 0 && !force) {
      return res.status(409).json({
        ok: false,
        error: `No se puede borrar: procedimiento referenciado en ${refs} cita(s). Usa ?force=1 si estás seguro.`,
      });
    }

    const r = await db.collection("procedimientos").deleteOne({ _id: oid });
    if (r.deletedCount === 0) return res.status(404).json({ ok: false, error: "No encontrado" });
    return res.json({ ok: true, deleted: r.deletedCount });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
