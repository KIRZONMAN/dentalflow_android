const express = require("express");
const { connect, oidMaybe } = require("../lib/mongo");
const router = express.Router();

// GET /api/insumos
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("insumos");

    const insumos = await col.find({}).sort({ nombre: 1 }).toArray();
    res.json({ ok: true, data: insumos });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// GET /api/insumos/:id
router.get("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("insumos");

    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "ID inválido" });

    const insumo = await col.findOne({ _id: oid });
    if (!insumo) {
      return res.status(404).json({ ok: false, error: "Insumo no encontrado" });
    }

    res.json({ ok: true, data: insumo });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
