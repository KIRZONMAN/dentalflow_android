// routes/ordenes_laboratorio.js
const express = require("express");
const { z } = require("zod");
const { connect, oidMaybe } = require("../lib/mongo");

const router = express.Router();

// ============================
// Helpers y Schemas (Zod)
// ============================
const parseDateStrict = (v) => {
  const d = v instanceof Date ? v : new Date(v);
  if (Number.isNaN(d.getTime())) throw new Error("fecha inválida");
  return d;
};

// Nota: el validador de la colección acepta observaciones como objeto o null.
// Aquí permitimos string y lo convertimos a { notas: "<string>" } por comodidad.
function normalizeObservaciones(v) {
  if (v == null) return null;
  if (typeof v === "string") return { notas: v.trim() };
  if (Array.isArray(v)) return { items: v };
  if (typeof v === "object") return v;
  return { valor: v };
}

const Producto = z.object({
  tipo_producto: z.string().min(1),
  especificaciones: z.string().optional(),
  cantidad: z.number().int().positive().default(1),
});

const OrdenLabCreate = z.object({
  cita_id: z.string().regex(/^[0-9a-fA-F]{24}$/),
  usuario_id: z.string().regex(/^[0-9a-fA-F]{24}$/),
  fecha_creacion: z
    .preprocess((v) => (v ? parseDateStrict(v) : new Date()), z.date())
    .optional(),
  estado: z.enum(["Pendiente", "En producción", "Listo para enviar", "Entregada", "Rechazada"]).default("Pendiente"),
  productos: z.array(Producto).min(1),
  observaciones: z.any().optional(), // se normaliza a objeto o null
});

// PATCH flexible:
// - estado?: enum
// - observaciones?: object|string (se normaliza)
// - set_productos?: Producto[] (reemplaza)
// - push_productos?: Producto[] (agrega)
// - producto_patch?: { index, item( parcial ) }
// - producto_delete_index?: number
const ProductoParcial = z.object({
  tipo_producto: z.string().min(1).optional(),
  especificaciones: z.string().optional(),
  cantidad: z.number().int().positive().optional(),
});
const ProductoPatchOp = z.object({
  index: z.number().int().nonnegative(),
  item: ProductoParcial
});

const OrdenLabPatch = z.object({
  estado: z.enum(["Pendiente", "En producción", "Listo para enviar", "Entregada", "Rechazada"]).optional(),
  observaciones: z.any().optional(),
  set_productos: z.array(Producto).min(1).optional(),
  push_productos: z.array(Producto).min(1).optional(),
  producto_patch: ProductoPatchOp.optional(),
  producto_delete_index: z.number().int().nonnegative().optional(),
}).refine(val =>
  val.estado !== undefined ||
  val.observaciones !== undefined ||
  val.set_productos !== undefined ||
  val.push_productos !== undefined ||
  val.producto_patch !== undefined ||
  val.producto_delete_index !== undefined
, { message: "Nada para actualizar" });

// ============================
// POST /api/ordenes-laboratorio
// ============================
router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("ordenes_laboratorio");

    const parsed = OrdenLabCreate.parse(req.body);
    const citaOid = oidMaybe(parsed.cita_id);
    const usuarioOid = oidMaybe(parsed.usuario_id);
    if (!citaOid || !usuarioOid) throw new Error("cita_id/usuario_id inválidos");

    const productos = parsed.productos.map((p) => ({
      tipo_producto: p.tipo_producto.trim(),
      especificaciones: p.especificaciones?.trim() ?? null,
      cantidad: p.cantidad ?? 1,
    }));

    const fechaCre = parsed.fecha_creacion || new Date();
    const obsNorm = normalizeObservaciones(parsed.observaciones);

    const now = new Date();
    const doc = {
      cita_id: citaOid,
      usuario_id: usuarioOid,
      fecha_creacion: fechaCre,           // requerido por tu validador
      estado: parsed.estado,
      observaciones: obsNorm,
      productos,
      // campos “extra” compatibles con lo que ya existe en BD (reportes)
      tipo: "laboratorio",
      fecha_expedicion: fechaCre,         // se indexa en ix_ol_fecha
      createdAt: now,
      updatedAt: now,
    };

    const r = await col.insertOne(doc);
    return res.status(201).json({ ok: true, id: r.insertedId.toString() });
  } catch (e) {
    return res.status(400).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/ordenes-laboratorio
// Filtros: ?cita_id=&usuario_id=&estado=&desde=YYYY-MM-DD&hasta=YYYY-MM-DD&q=texto
// Paginación: ?page=&limit=
// ============================
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("ordenes_laboratorio");

    const { cita_id, usuario_id, estado, desde, hasta, q } = req.query;
    let { page, limit } = req.query;

    const query = {};
    if (cita_id) {
      const coid = oidMaybe(String(cita_id));
      if (!coid) return res.status(400).json({ ok: false, error: "cita_id inválido" });
      query.cita_id = coid;
    }
    if (usuario_id) {
      const uoid = oidMaybe(String(usuario_id));
      if (!uoid) return res.status(400).json({ ok: false, error: "usuario_id inválido" });
      query.usuario_id = uoid;
    }
    if (estado) {
      const estados = ["Pendiente", "En producción", "Listo para enviar", "Entregada", "Rechazada"];
      if (!estados.includes(String(estado))) {
        return res.status(400).json({ ok: false, error: "estado inválido" });
      }
      query.estado = String(estado);
    }
    if (desde || hasta) {
      query.fecha_creacion = {};
      if (desde) query.fecha_creacion.$gte = parseDateStrict(`${desde}T00:00:00`);
      if (hasta) query.fecha_creacion.$lte = parseDateStrict(`${hasta}T23:59:59`);
    }
    if (q) {
      const rx = new RegExp(String(q), "i");
      query.$or = [
        { "productos.tipo_producto": rx },
        { "productos.especificaciones": rx },
        { "observaciones.notas": rx },
      ];
    }

    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const cursor = col.find(query).sort({ fecha_creacion: -1 }).skip((page - 1) * limit).limit(limit);
    const [data, total] = await Promise.all([cursor.toArray(), col.countDocuments(query)]);
    return res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// GET /api/ordenes-laboratorio/:id
// ============================
router.get("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const db = await connect();
    const doc = await db.collection("ordenes_laboratorio").findOne({ _id: oid });
    if (!doc) return res.status(404).json({ ok: false, error: "Orden no encontrada" });

    return res.json({ ok: true, data: doc });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

// ============================
// PATCH /api/ordenes-laboratorio/:id
// Soporta:
//  - estado
//  - observaciones (replace, normalizado)
//  - set_productos (reemplaza todo el array)
//  - push_productos (agrega items)
//  - producto_patch { index, item } (merge de un item puntual)
//  - producto_delete_index (elimina 1 item por índice)
// ============================
router.patch("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const patch = OrdenLabPatch.parse(req.body);
    const db = await connect();
    const col = db.collection("ordenes_laboratorio");

    // si reemplaza todo:
    if (patch.set_productos) {
      const productos = patch.set_productos.map((p) => ({
        tipo_producto: p.tipo_producto.trim(),
        especificaciones: p.especificaciones?.trim() ?? null,
        cantidad: p.cantidad ?? 1,
      }));
      const r = await col.updateOne(
        { _id: oid },
        {
          $set: {
            productos,
            ...(patch.estado ? { estado: patch.estado } : {}),
            ...(patch.observaciones !== undefined
              ? { observaciones: normalizeObservaciones(patch.observaciones) }
              : {}),
            updatedAt: new Date(),
          },
        }
      );
      if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "Orden no encontrada" });
      return res.json({ ok: true, modified: r.modifiedCount });
    }

    // si agrega items:
    if (patch.push_productos) {
      const productos = patch.push_productos.map((p) => ({
        tipo_producto: p.tipo_producto.trim(),
        especificaciones: p.especificaciones?.trim() ?? null,
        cantidad: p.cantidad ?? 1,
      }));
      const r = await col.updateOne(
        { _id: oid },
        {
          $push: { productos: { $each: productos } },
          $set: {
            ...(patch.estado ? { estado: patch.estado } : {}),
            ...(patch.observaciones !== undefined
              ? { observaciones: normalizeObservaciones(patch.observaciones) }
              : {}),
            updatedAt: new Date(),
          },
        }
      );
      if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "Orden no encontrada" });
      return res.json({ ok: true, modified: r.modifiedCount });
    }

    // si edita un item puntual por índice:
    if (patch.producto_patch) {
      const doc = await col.findOne({ _id: oid }, { projection: { productos: 1 } });
      if (!doc) return res.status(404).json({ ok: false, error: "Orden no encontrada" });

      const idx = patch.producto_patch.index;
      const curr = (doc.productos || [])[idx];
      if (!curr) return res.status(404).json({ ok: false, error: "Índice fuera de rango" });

      const merged = {
        ...curr,
        ...(patch.producto_patch.item.tipo_producto != null
          ? { tipo_producto: patch.producto_patch.item.tipo_producto.trim() }
          : {}),
        ...(patch.producto_patch.item.especificaciones != null
          ? { especificaciones: patch.producto_patch.item.especificaciones.trim() }
          : {}),
        ...(patch.producto_patch.item.cantidad != null
          ? { cantidad: patch.producto_patch.item.cantidad }
          : {}),
      };

      // Validar contra schema completo del item
      Producto.parse(merged);

      const r = await col.updateOne(
        { _id: oid },
        {
          $set: {
            [`productos.${idx}`]: merged,
            ...(patch.estado ? { estado: patch.estado } : {}),
            ...(patch.observaciones !== undefined
              ? { observaciones: normalizeObservaciones(patch.observaciones) }
              : {}),
            updatedAt: new Date(),
          },
        }
      );
      return res.json({ ok: true, modified: r.modifiedCount });
    }

    // si elimina un item por índice:
    if (patch.producto_delete_index !== undefined) {
      const idx = patch.producto_delete_index;
      // unset y pull para evitar reindexado incorrecto
      await col.updateOne({ _id: oid }, { $unset: { [`productos.${idx}`]: 1 } });
      const r2 = await col.updateOne(
        { _id: oid },
        {
          $pull: { productos: null },
          $set: {
            ...(patch.estado ? { estado: patch.estado } : {}),
            ...(patch.observaciones !== undefined
              ? { observaciones: normalizeObservaciones(patch.observaciones) }
              : {}),
            updatedAt: new Date(),
          },
        }
      );
      return res.json({ ok: true, modified: r2.modifiedCount > 0 ? 1 : 0 });
    }

    // estado / observaciones solamente
    const r = await col.updateOne(
      { _id: oid },
      {
        $set: {
          ...(patch.estado ? { estado: patch.estado } : {}),
          ...(patch.observaciones !== undefined
            ? { observaciones: normalizeObservaciones(patch.observaciones) }
            : {}),
          updatedAt: new Date(),
        },
      }
    );
    if (r.matchedCount === 0) return res.status(404).json({ ok: false, error: "Orden no encontrada" });
    return res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    const msg = e?.errors ? JSON.stringify(e.errors, null, 2) : e.message;
    return res.status(400).json({ ok: false, error: msg });
  }
});

// ============================
// DELETE /api/ordenes-laboratorio/:id  (hard delete)
// ============================
router.delete("/:id", async (req, res) => {
  try {
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "id inválido" });

    const db = await connect();
    const r = await db.collection("ordenes_laboratorio").deleteOne({ _id: oid });
    if (r.deletedCount === 0) return res.status(404).json({ ok: false, error: "Orden no encontrada" });

    return res.json({ ok: true, deleted: r.deletedCount });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
