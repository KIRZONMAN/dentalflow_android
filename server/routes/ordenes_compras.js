const express = require("express");
const { z } = require("zod");
const { connect, oidMaybe } = require("../lib/mongo");

const router = express.Router();

const Detalle = z.object({
  insumo_id: z.string().regex(/^[0-9a-fA-F]{24}$/),
  cantidad: z.number().int().positive(),
  precio_unit: z.number().nonnegative()
});

const OrdenCompraCreate = z.object({
  proveedor_id: z.string().min(1),
  usuario_id: z.string().regex(/^[0-9a-fA-F]{24}$/),
  estado: z.enum(["Borrador", "Enviada", "Recibida", "Anulada"]).default("Enviada"),
  fecha_expedicion: z.preprocess((v) => (v ? new Date(v) : new Date()), z.date()).optional(),
  fecha_vencimiento: z.preprocess((v) => (v ? new Date(v) : null), z.date().nullable()).optional(),
  detalles: z.array(Detalle).min(1),
  observaciones: z.string().optional()
});

router.post("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("ordenes_compras");

    const parsed = OrdenCompraCreate.parse(req.body);
    const usuarioOid = oidMaybe(parsed.usuario_id);
    if (!usuarioOid) throw new Error("usuario_id inválido");

    const detalles = parsed.detalles.map(d => ({
      insumo_id: oidMaybe(d.insumo_id),
      cantidad: d.cantidad,
      precio_unit: d.precio_unit,
      subtotal: d.cantidad * d.precio_unit
    }));

    if (detalles.some(d => !d.insumo_id)) throw new Error("insumo_id inválido en detalles");

    const total = detalles.reduce((acc, d) => acc + d.subtotal, 0);

    const doc = {
      tipo: "Insumos",
      proveedor_id: parsed.proveedor_id.trim(),
      usuario_id: usuarioOid,
      estado: parsed.estado,
      fecha_expedicion: parsed.fecha_expedicion || new Date(),
      fecha_vencimiento: parsed.fecha_vencimiento ?? null,
      detalles,
      total,
      observaciones: parsed.observaciones?.trim() || null,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    const r = await col.insertOne(doc);
    res.status(201).json({
      ok: true,
      data: {
        id: r.insertedId.toString(),
        total
      }
    });

  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
});

// GET /api/ordenes-compras?proveedor_id=...&desde=YYYY-MM-DD&hasta=YYYY-MM-DD
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("ordenes_compras");
    const { proveedor_id, desde, hasta } = req.query;

    const q = {};
    if (proveedor_id) q.proveedor_id = String(proveedor_id);
    if (desde || hasta) {
      q.fecha_expedicion = {};
      if (desde) q.fecha_expedicion.$gte = new Date(`${desde}T00:00:00`);
      if (hasta) q.fecha_expedicion.$lte = new Date(`${hasta}T23:59:59`);
    }

    const docs = await col.find(q).sort({ fecha_expedicion: -1 }).limit(100).toArray();
    res.json({ ok: true, data: docs });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
