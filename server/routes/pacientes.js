// routes/pacientes.js
const express = require("express");
const { z } = require("zod");
const router = express.Router();
const { connect } = require("../lib/mongo");
const { pacienteSchemaUpsert, normalizePaciente, titleCase } = require("../lib/validate");

// ============================
// Schemas (Zod)
// ============================
const PacientePatch = z.object({
  nombres: z.string().min(1).optional(),
  apellidos: z.string().min(1).optional(),
  edad: z.number().int().min(0).max(120).optional(),
  genero: z.string().min(1).optional(),
  telefono: z.string().optional(),
  direccion: z.string().optional(),
  correo: z.string().email().optional(),
  tipo_sangre: z.string().min(1).optional(),
}).refine(obj => Object.keys(obj).length > 0, { message: "Nada para actualizar" });

function normalizePatch(p) {
  const out = {};
  if (p.nombres != null) out.nombres = titleCase(p.nombres);
  if (p.apellidos != null) out.apellidos = titleCase(p.apellidos);
  if (p.edad != null) out.edad = p.edad;
  if (p.genero != null) out.genero = titleCase(p.genero);
  if (p.telefono != null) out.telefono = String(p.telefono).trim();
  if (p.direccion != null) out.direccion = String(p.direccion).trim();
  if (p.correo != null) out.correo = p.correo.trim().toLowerCase();
  if (p.tipo_sangre != null) out.tipo_sangre = p.tipo_sangre.toUpperCase().replace(/\s+/g, "");
  return out;
}

// ============================
// GET /api/pacientes
// ?q=texto&page=1&limit=50
// Busca por cédula (_id), nombres, apellidos, correo (case-insensitive)
// ============================
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("pacientes");
    const qtext = (req.query.q || "").trim();
    let { page, limit } = req.query;

    const query = qtext
      ? {
          $or: [
            { _id: { $regex: qtext, $options: "i" } },
            { nombres: { $regex: qtext, $options: "i" } },
            { apellidos: { $regex: qtext, $options: "i" } },
            { correo: { $regex: qtext, $options: "i" } },
          ],
        }
      : {};

    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const cursor = col
      .find(query, { projection: { createdAt: 0, updatedAt: 0 } })
      .sort({ apellidos: 1, nombres: 1 })
      .skip((page - 1) * limit)
      .limit(limit);

    const [data, total] = await Promise.all([cursor.toArray(), col.countDocuments(query)]);
    res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/pacientes/:id   (id = cédula, string)
// ============================
router.get("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("pacientes");
    const doc = await col.findOne({ _id: String(req.params.id) });
    if (!doc) return res.status(404).json({ ok: false, error: "Paciente no encontrado" });
    res.json({ ok: true, data: doc });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// POST /api/pacientes  (upsert por _id = cédula)
// ============================
router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("pacientes");
    const parsed = pacienteSchemaUpsert.parse(req.body);
    const norm = normalizePaciente(parsed);

    const now = new Date();
    const r = await col.updateOne(
      { _id: norm._id },
      {
        $setOnInsert: { createdAt: now },
        $set: { ...norm, updatedAt: now },
      },
      { upsert: true }
    );

    res.status(r.upsertedCount ? 201 : 200).json({ ok: true, upserted: !!r.upsertedCount });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// PATCH /api/pacientes/:id  (parcial)
// ============================
router.patch("/:id", async (req, res) => {
  try {
    const patch = PacientePatch.parse(req.body);
    const $set = { ...normalizePatch(patch), updatedAt: new Date() };

    const db = await connect();
    const col = db.collection("pacientes");
    const r = await col.updateOne({ _id: String(req.params.id) }, { $set });
    if (r.matchedCount === 0) {
      return res.status(404).json({ ok: false, error: "Paciente no encontrado" });
    }
    res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// DELETE /api/pacientes/:id
// Protegido si hay referencias en citas o historia clínica.
// ============================
router.delete("/:id", async (req, res) => {
  try {
    const id = String(req.params.id);
    const db = await connect();

    const [citasRef, historiaRef] = await Promise.all([
      db.collection("citas").countDocuments({ paciente_id: id }),
      db.collection("historias_clinicas").countDocuments({ paciente_id: id }),
    ]);

    const refs = (citasRef || 0) + (historiaRef || 0);
    if (refs > 0) {
      return res.status(409).json({
        ok: false,
        error: "No se puede eliminar: el paciente tiene referencias activas",
        refs: { citas: citasRef, historia: historiaRef },
      });
    }

    const r = await db.collection("pacientes").deleteOne({ _id: id });
    if (r.deletedCount === 0) {
      return res.status(404).json({ ok: false, error: "Paciente no encontrado" });
    }
    res.json({ ok: true, deleted: r.deletedCount });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
