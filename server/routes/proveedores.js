const express = require("express");
const { connect } = require("../lib/mongo");
const router = express.Router();

// GET /api/proveedores
router.get("/", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("proveedores");

    const proveedores = await col.find({}).sort({ nombre: 1 }).toArray();
    res.json({ ok: true, data: proveedores });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// GET /api/proveedores/:id
router.get("/:id", async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("proveedores");

    const proveedor = await col.findOne({ _id: req.params.id });

    if (!proveedor) {
      return res.status(404).json({ ok: false, error: "Proveedor no encontrado" });
    }

    res.json({ ok: true, data: proveedor });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

module.exports = router;
