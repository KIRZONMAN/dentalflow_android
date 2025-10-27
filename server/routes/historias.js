// routes/historias.js
const express = require("express");
const { z } = require("zod");
const { connect } = require("../lib/mongo");

const router = express.Router();

// ============================
// Schemas (Zod)
// ============================
const parseDateStrict = (v) => {
  const d = v instanceof Date ? v : new Date(v);
  if (Number.isNaN(d.getTime())) throw new Error("fecha inválida");
  return d;
};

const ProcRealizado = z.object({
  tratamiento: z.string().min(1),
  fecha: z.preprocess(parseDateStrict, z.date()),
  odontologo: z.string().optional(),
  resultado: z.string().optional(),
});

// POST append: 1 o varios (con alias legacy)
const HistoriaAppend = z
  .object({
    paciente_id: z.string().min(1),
    procedimiento: ProcRealizado.optional(),                // 1 item
    procedimientos_realizados: z.array(ProcRealizado).optional(), // varios
    // alias legacy (si llega, lo mapeamos antes de validar)
    tratamientos_realizados: z.array(ProcRealizado).optional(),
  })
  .refine(
    (v) =>
      v.procedimiento ||
      (Array.isArray(v.procedimientos_realizados) && v.procedimientos_realizados.length > 0) ||
      (Array.isArray(v.tratamientos_realizados) && v.tratamientos_realizados.length > 0),
    { message: "Debe incluir 'procedimiento' o 'procedimientos_realizados' (o alias 'tratamientos_realizados')" }
  );

// PATCH de un procedimiento puntual (merge parcial)
const ProcPatch = z.object({
  tratamiento: z.string().min(1).optional(),
  fecha: z.preprocess(parseDateStrict, z.date()).optional(),
  odontologo: z.string().optional(),
  resultado: z.string().optional(),
}).refine((v) => Object.keys(v).length > 0, { message: "Nada para actualizar" });

// ============================
// GET /api/historias
// ?paciente_id=...  (si no viene, lista paginada de ids)
// ============================
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const { paciente_id } = req.query;

    if (paciente_id) {
      const h = await col.findOne({ paciente_id: String(paciente_id) });
      if (!h) return res.status(404).json({ ok: false, error: "Historia no encontrada" });
      return res.json({ ok: true, data: h });
    }

    // Lista compacta (solo ids) para navegar
    let { page, limit } = req.query;
    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const cursor = col
      .find({}, { projection: { _id: 0, paciente_id: 1, updatedAt: 1 } })
      .sort({ updatedAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit);

    const [data, total] = await Promise.all([cursor.toArray(), col.countDocuments({})]);
    return res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/historias/:paciente_id
// ============================
router.get("/:paciente_id", async (req, res) => {
  try {
    const db = await connect();
    const h = await db.collection("historias_clinicas").findOne({
      paciente_id: String(req.params.paciente_id),
    });
    if (!h) return res.status(404).json({ ok: false, error: "Historia no encontrada" });
    return res.json({ ok: true, data: h });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/historias/:paciente_id/procedimientos
// Filtra por fecha: ?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
// ============================
router.get("/:paciente_id/procedimientos", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");
    const { paciente_id } = req.params;
    const { desde, hasta } = req.query;

    const match = { paciente_id: String(paciente_id) };

    const conds = [];
    if (desde) conds.push({ $gte: ["$$p.fecha", parseDateStrict(`${desde}T00:00:00`)] });
    if (hasta) conds.push({ $lte: ["$$p.fecha", parseDateStrict(`${hasta}T23:59:59`)] });

    const pipeline = [
      { $match: match },
      {
        $project: {
          _id: 0,
          paciente_id: 1,
          procedimientos: {
            $filter: {
              input: "$procedimientos_realizados",
              as: "p",
              cond:
                conds.length === 0
                  ? { $literal: true }
                  : conds.length === 1
                  ? conds[0]
                  : { $and: conds },
            },
          },
        },
      },
    ];

    const out = await col.aggregate(pipeline).toArray();
    if (out.length === 0) return res.status(404).json({ ok: false, error: "Historia no encontrada" });
    return res.json({ ok: true, data: out[0] });
  } catch (e) {
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// POST /api/historias  (append upsert)
// Acepta: { paciente_id, procedimiento } o { paciente_id, procedimientos_realizados: [...] }
// También acepta alias legacy: tratamientos_realizados
// ============================
router.post("/", async (req, res) => {
  try {
    // Mapeo alias legacy -> nuevo nombre
    if (!req.body.procedimientos_realizados && Array.isArray(req.body.tratamientos_realizados)) {
      req.body.procedimientos_realizados = req.body.tratamientos_realizados;
      delete req.body.tratamientos_realizados;
    }

    const parsed = HistoriaAppend.parse(req.body);
    const items =
      parsed.procedimientos_realizados ??
      (parsed.procedimiento ? [parsed.procedimiento] : []);

    const now = new Date();
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const r = await col.updateOne(
      { paciente_id: parsed.paciente_id.trim() },
      {
        $setOnInsert: { paciente_id: parsed.paciente_id.trim(), createdAt: now },
        $push: { procedimientos_realizados: { $each: items } },
        $set: { updatedAt: now },
      },
      { upsert: true }
    );

    return res.status(200).json({
      ok: true,
      upserted: !!r.upsertedId,
      added: items.length,
    });
  } catch (e) {
    const msg = e?.errors ? JSON.stringify(e.errors, null, 2) : e.message;
    return res.status(400).json({ ok: false, error: msg });
  }
});

// ============================
// PATCH /api/historias/:paciente_id/procedimientos/:index
// Edita un procedimiento por índice (0-based).
// Body: ProcPatch (merge con el existente)
// ============================
router.patch("/:paciente_id/procedimientos/:index", async (req, res) => {
  try {
    const idx = Number.parseInt(req.params.index, 10);
    if (Number.isNaN(idx) || idx < 0) {
      return res.status(400).json({ ok: false, error: "index inválido" });
    }

    const patch = ProcPatch.parse(req.body);
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const h = await col.findOne({ paciente_id: String(req.params.paciente_id) });
    if (!h) return res.status(404).json({ ok: false, error: "Historia no encontrada" });

    const curr = (h.procedimientos_realizados || [])[idx];
    if (!curr) return res.status(404).json({ ok: false, error: "Índice fuera de rango" });

    // Merge y revalidar completo
    const merged = ProcRealizado.parse({
      ...curr,
      ...patch,
    });

    const field = `procedimientos_realizados.${idx}`;
    const r = await col.updateOne(
      { _id: h._id },
      { $set: { [field]: merged, updatedAt: new Date() } }
    );

    return res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    const msg = e?.errors ? JSON.stringify(e.errors, null, 2) : e.message;
    return res.status(400).json({ ok: false, error: msg });
  }
});

// ============================
// DELETE /api/historias/:paciente_id/procedimientos/:index
// Borra un procedimiento por índice (0-based) de forma atómica (unset + pull)
// ============================
router.delete("/:paciente_id/procedimientos/:index", async (req, res) => {
  try {
    const idx = Number.parseInt(req.params.index, 10);
    if (Number.isNaN(idx) || idx < 0) {
      return res.status(400).json({ ok: false, error: "index inválido" });
    }

    const db = await connect();
    const col = db.collection("historias_clinicas");

    const h = await col.findOne({ paciente_id: String(req.params.paciente_id) });
    if (!h) return res.status(404).json({ ok: false, error: "Historia no encontrada" });

    const curr = (h.procedimientos_realizados || [])[idx];
    if (!curr) return res.status(404).json({ ok: false, error: "Índice fuera de rango" });

    // Dos pasos: $unset (deja un hueco null) + $pull para quitar nulls
    await col.updateOne({ _id: h._id }, { $unset: { [`procedimientos_realizados.${idx}`]: 1 } });
    const r2 = await col.updateOne({ _id: h._id }, { $pull: { procedimientos_realizados: null }, $set: { updatedAt: new Date() } });

    return res.json({ ok: true, deleted: r2.modifiedCount > 0 });
  } catch (e) {
    return res.status(400).json({ ok: false, error: e.message });
  }
});

module.exports = router;
