const express = require("express");
const { z } = require("zod");
const { connect, oidMaybe } = require("../lib/mongo");

const router = express.Router();

// ============================ 
// Helpers
// ============================
const ESTADOS = ["Pendiente", "Confirmada", "Cancelada", "Completada"];

const parseDateStrict = (v) => {
  const d = v instanceof Date ? v : new Date(v);
  if (Number.isNaN(d.getTime())) throw new Error("fecha inválida");
  return d;
};

const asNumber = (v) => {
  const n = typeof v === "string" ? Number(v) : v;
  if (Number.isNaN(n)) throw new Error("valor numérico inválido");
  return n;
};

const normalizeProcs = (arr) => {
  const list = Array.isArray(arr) ? arr : [];
  return list.map((p) => ({
    nombre: String(p.nombre || "").trim(),
    costo_unitario: asNumber(p.costo_unitario),
    cantidad: p.cantidad != null ? parseInt(p.cantidad, 10) : 1,
  })).filter(p => p.nombre && p.costo_unitario >= 0 && p.cantidad >= 1);
};

const computeTotal = (procs) =>
  (procs || []).reduce((acc, p) => acc + (p.costo_unitario * (p.cantidad ?? 1)), 0);

// ============================
// Schemas (Zod)
// ============================
const Proc = z.object({
  nombre: z.string().min(1),
  costo_unitario: z.preprocess(asNumber, z.number().nonnegative()),
  cantidad: z.preprocess((v) => (v == null ? 1 : Number.parseInt(v, 10)), z.number().int().min(1)).optional().default(1),
});

const CitaCreate = z.object({
  fecha: z.preprocess(parseDateStrict, z.date()),
  paciente_id: z.string().min(1), // cédula (string) — decisión actual
  usuario_id: z.string().regex(/^[0-9a-fA-F]{24}$/),
  estado: z.enum(ESTADOS).optional(),
  motivo: z.string().optional(),
  procedimientos: z.array(Proc).optional(),
  total: z.preprocess(asNumber, z.number().nonnegative()).optional().default(0),
});

const CitaPatch = z.object({
  fecha: z.preprocess((v) => {
    if (!v) return undefined;
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) throw new Error("fecha inválida");
    return d;
  }, z.date()).optional(),
  paciente_id: z.string().min(1).optional(),
  usuario_id: z.string().regex(/^[0-9a-fA-F]{24}$/).optional(),
  estado: z.enum(ESTADOS).optional(),
  motivo: z.string().optional(),
  procedimientos: z.array(Proc).optional(),
  total: z.preprocess(asNumber, z.number().nonnegative()).optional(),
}).refine(
  (v) => Object.keys(v).length > 0,
  { message: "Nada para actualizar" }
);

// ============================
// POST /api/citas
// ============================
router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");

    const parsed = CitaCreate.parse(req.body);

    const { ObjectId } = require("mongodb");
    const usuarioOid = new ObjectId(parsed.usuario_id.trim());

    if (!usuarioOid) throw new Error("usuario_id inválido");

    const procs = normalizeProcs(parsed.procedimientos || []);
    const totalCalc = computeTotal(procs);

    const doc = {
      fecha: parsed.fecha,
      paciente_id: parsed.paciente_id.trim(),
      usuario_id: usuarioOid,
      estado: parsed.estado || "Pendiente",
      motivo: parsed.motivo?.trim() || null,
      procedimientos: procs,
      total: parsed.total ?? totalCalc,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    const r = await col.insertOne(doc);
    return res.status(201).json({ ok: true, id: r.insertedId.toString(), total: doc.total });
  } catch (e) {
    const msg = e?.errors ? JSON.stringify(e.errors, null, 2) : e.message;
    return res.status(400).json({ ok: false, error: msg });
  }
});

// ============================
// GET /api/citas
// Filtros: ?paciente_id=&usuario_id=&estado=&desde=YYYY-MM-DD&hasta=YYYY-MM-DD&limit=&page=
// ============================
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");

    const { paciente_id, usuario_id, estado, desde, hasta } = req.query;
    let { limit, page } = req.query;

    const q = {};
    if (paciente_id) q.paciente_id = String(paciente_id);
    if (usuario_id) {
      const oid = oidMaybe(usuario_id);
      if (!oid) return res.status(400).json({ ok: false, error: "usuario_id inválido" });
      q.usuario_id = String(usuario_id);
    }
    if (estado) {
      if (!ESTADOS.includes(String(estado))) {
        return res.status(400).json({ ok: false, error: "estado inválido" });
      }
      q.estado = estado;
    }
    if (desde || hasta) {
      q.fecha = {};
      if (desde) q.fecha.$gte = new Date(`${desde}T00:00:00`);
      if (hasta) q.fecha.$lte = new Date(`${hasta}T23:59:59`);
    }

    limit = Math.min(Math.max(parseInt(limit ?? "100", 10), 1), 500);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    // 🔍 Lookup para traer nombre del paciente
    const data = await col.aggregate([
      { $match: q },
      { $sort: { fecha: -1 } },
      { $skip: (page - 1) * limit },
      { $limit: limit },
      {
        $lookup: {
          from: "pacientes",
          localField: "paciente_id",
          foreignField: "_id",
          as: "paciente",
        },
      },
      { $unwind: { path: "$paciente", preserveNullAndEmptyArrays: true } },
      {
        $addFields: {
          paciente_nombre: {
            $concat: [
              { $ifNull: ["$paciente.nombres", ""] },
              " ",
              { $ifNull: ["$paciente.apellidos", ""] },
            ],
          },
        },
      },
      {
        $project: {
          paciente: 0, // no se devuelve el objeto completo del paciente
        },
      },
    ]).toArray();

    const total = await col.countDocuments(q);
    return res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET citasHoy
router.get("/hoy", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");

    const { usuario_id } = req.query;
    let { limit, page } = req.query;

    const ahora = new Date();

    // Comenzamos desde ahora
    const q = {
      fecha: { $gte: ahora }, // solo citas futuras
    };

    if (usuario_id) {
      const oid = oidMaybe(usuario_id);
      if (!oid) return res.status(400).json({ ok: false, error: "usuario_id inválido" });
      q.usuario_id = String(usuario_id);
    }

    limit = Math.min(Math.max(parseInt(limit ?? "100", 10), 1), 500);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const data = await col.aggregate([
      { $match: q },
      { $sort: { fecha: 1 } }, // próximas primero
      { $skip: (page - 1) * limit },
      { $limit: limit },
      {
        $lookup: {
          from: "pacientes",
          localField: "paciente_id",
          foreignField: "_id",
          as: "paciente",
        },
      },
      { $unwind: { path: "$paciente", preserveNullAndEmptyArrays: true } },
      {
        $addFields: {
          paciente_nombre: {
            $concat: [
              { $ifNull: ["$paciente.nombres", ""] },
              " ",
              { $ifNull: ["$paciente.apellidos", ""] },
            ],
          },
        },
      },
      { $project: { paciente: 0 } },
    ]).toArray();

    const total = await col.countDocuments(q);
    return res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// STREAM DE CITAS EN TIEMPO REAL
router.get("/stream", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");

    res.setHeader("Content-Type", "text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");
    res.flushHeaders();

    // Change Stream
    const changeStream = col.watch([], { fullDocument: "updateLookup" });

    changeStream.on("change", (change) => {
      res.write(`data: ${JSON.stringify(change)}\n\n`);
    });

    changeStream.on("error", (err) => {
      console.error("❌ Error en Change Stream:", err);
      res.write(`event: error\ndata: "${err.message}"\n\n`);
    });

    req.on("close", () => {
      changeStream.close();
      res.end();
    });

  } catch (err) {
    console.error("❌ Error SSE:", err);
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Obtener una cita específica con el nombre del paciente
router.get("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");
    const { id } = req.params;

    const oid = oidMaybe(id);
    if (!oid) return res.status(400).json({ ok: false, error: "ID inválido" });

    const data = await col.aggregate([
      { $match: { _id: oid } },
      {
        $lookup: {
          from: "pacientes",
          localField: "paciente_id",
          foreignField: "_id",
          as: "paciente",
        },
      },
      { $unwind: { path: "$paciente", preserveNullAndEmptyArrays: true } },
      {
        $addFields: {
          paciente_nombre: {
            $concat: [
              { $ifNull: ["$paciente.nombres", ""] },
              " ",
              { $ifNull: ["$paciente.apellidos", ""] },
            ],
          },
        },
      },
      {
        $project: {
          paciente: 0,
        },
      },
    ]).toArray();

    if (!data.length) return res.status(404).json({ ok: false, error: "Cita no encontrada" });

    return res.json({ ok: true, data: data[0] });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ ok: false, error: e.message });
  }
});
// ============================
// PATCH /api/citas/:id
// - Recalcula total si cambian procedimientos o si viene 'total' explícito
// ============================
router.patch("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");
    const _id = oidMaybe(req.params.id);
    if (!_id) return res.status(400).json({ ok: false, error: "id inválido" });

    const parsed = CitaPatch.parse(req.body);

    const $set = { updatedAt: new Date() };

    if (parsed.fecha) $set.fecha = parsed.fecha;
    if (parsed.paciente_id) $set.paciente_id = parsed.paciente_id.trim();
    if (parsed.usuario_id) {
      const oid = oidMaybe(parsed.usuario_id);
      if (!oid) return res.status(400).json({ ok: false, error: "usuario_id inválido" });
      $set.usuario_id = oid;
    }
    if (parsed.estado) $set.estado = parsed.estado;
    if (parsed.motivo !== undefined) $set.motivo = parsed.motivo?.trim() || null;

    let procs;
    if (parsed.procedimientos) {
      procs = normalizeProcs(parsed.procedimientos);
      $set.procedimientos = procs;
    }

    // total: explícito o recalculado si cambian procedimientos
    if (parsed.total != null) {
      $set.total = parsed.total;
    } else if (procs) {
      $set.total = computeTotal(procs);
    }

    const r = await col.updateOne({ _id }, { $set });
    if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "Cita no encontrada" });

    return res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    const msg = e?.errors ? JSON.stringify(e.errors, null, 2) : e.message;
    return res.status(400).json({ ok: false, error: msg });
  }
});

// ============================
// DELETE /api/citas/:id
// - ?soft=true  -> cambia estado a "Cancelada" (soft delete)
// - default     -> hard delete
// ============================
router.delete("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("citas");
    const _id = oidMaybe(req.params.id);
    if (!_id) return res.status(400).json({ ok: false, error: "id inválido" });

    const soft = String(req.query.soft || "").toLowerCase() === "true";

    if (soft) {
      const r = await col.updateOne({ _id }, { $set: { estado: "Cancelada", updatedAt: new Date() } });
      if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "Cita no encontrada" });
      return res.json({ ok: true, softDeleted: true });
    } else {
      const r = await col.deleteOne({ _id });
      if (r.deletedCount === 0) return res.status(404).json({ ok: false, error: "Cita no encontrada" });
      return res.json({ ok: true, deleted: true });
    }
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
