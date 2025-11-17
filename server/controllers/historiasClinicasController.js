const { connect, oidMaybe } = require("../lib/mongo");

// ============================
// Helper para normalizar fechas
// ============================
function normalizeDate(value) {
  if (!value) return null;

  // Si es tipo objeto BSON {"$date": "..."}
  if (typeof value === "object" && value.$date) {
    return new Date(value.$date);
  }

  // Si ya es una fecha válida en string o Date
  const d = new Date(value);
  return isNaN(d.getTime()) ? null : d;
}

// ============================
// Helper para normalizar arrays
// ============================
function normalizeArray(value) {
  if (!Array.isArray(value)) return [];

  // Si el array contiene fechas, convertirlas
  return value.map((item) => {
    if (typeof item === "object" && item !== null) {
      const newItem = { ...item };
      // Normalizamos todas las propiedades que parezcan fechas
      for (const k of Object.keys(newItem)) {
        if (k.toLowerCase().includes("fecha")) {
          const parsed = normalizeDate(newItem[k]);
          if (parsed) newItem[k] = parsed;
        }
      }
      return newItem;
    }
    return item;
  });
}

// ===============================================
// GET /api/historias-clinicas
// ===============================================
exports.listarHistorias = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const pacienteId = (req.query.paciente_id || "").trim();
    let { page, limit } = req.query;
    limit = Math.min(Math.max(parseInt(limit ?? "50", 10), 1), 200);
    page = Math.max(parseInt(page ?? "1", 10), 1);

    const q = pacienteId ? { paciente_id: pacienteId } : {};

    const cursor = col.find(q).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit);
    const [data, total] = await Promise.all([
      cursor.toArray(),
      col.countDocuments(q),
    ]);

    res.json({ ok: true, total, page, pageSize: limit, data });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
};

// ===============================================
// GET /api/historias-clinicas/:id
// ===============================================
exports.obtenerHistoria = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "ID inválido" });

    const doc = await col.findOne({ _id: oid });
    if (!doc) return res.status(404).json({ ok: false, error: "Historia clínica no encontrada" });

    doc._id = doc._id.toString();
    res.json({ ok: true, data: doc });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
};

// GET /api/historias-clinicas/paciente/:paciente_id
exports.obtenerHistoriaPaciente = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const pacienteId = String(req.params.paciente_id).trim();

    const historia = await col.findOne({ paciente_id: pacienteId });

    if (!historia) {
      return res.json({
        ok: false, error: "Historia no encontrada", params: req.params,
        query: req.query,
        body: req.body,
      });
    }

    return res.json({ ok: true, data: historia });
  } catch (e) {
    return res.status(500).json({ ok: false, error: e.message });
  }
};



// ===============================================
// POST /api/historias-clinicas
// ===============================================
exports.crearHistoria = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");

    const {
      paciente_id,
      antecedentes_medicos = [],
      alergias = [],
      recetas_medicas = [],
      procedimientos_realizados = [],
    } = req.body;

    if (!paciente_id) {
      return res.status(400).json({ ok: false, error: "El campo paciente_id es obligatorio" });
    }

    const now = new Date();
    const doc = {
      paciente_id,
      antecedentes_medicos: normalizeArray(antecedentes_medicos),
      alergias: normalizeArray(alergias),
      recetas_medicas: normalizeArray(recetas_medicas),
      procedimientos_realizados: normalizeArray(procedimientos_realizados),
      createdAt: now,
      updatedAt: now,
    };

    const r = await col.insertOne(doc);
    res.status(201).json({ ok: true, id: r.insertedId.toString() });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
};

// ===============================================
// PATCH /api/historias-clinicas/:id
// ===============================================
exports.actualizarHistoria = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "ID inválido" });

    const updateData = {};
    const campos = [
      "paciente_id",
      "antecedentes_medicos",
      "alergias",
      "recetas_medicas",
      "procedimientos_realizados",
    ];

    for (const campo of campos) {
      if (req.body[campo] != null) {
        if (Array.isArray(req.body[campo])) {
          updateData[campo] = normalizeArray(req.body[campo]);
        } else {
          updateData[campo] = req.body[campo];
        }
      }
    }

    if (Object.keys(updateData).length === 0) {
      return res.status(400).json({ ok: false, error: "Nada para actualizar" });
    }

    updateData.updatedAt = new Date();

    const r = await col.updateOne({ _id: oid }, { $set: updateData });
    if (r.matchedCount === 0) {
      return res.status(404).json({ ok: false, error: "Historia clínica no encontrada" });
    }

    res.json({ ok: true, modified: r.modifiedCount });
  } catch (e) {
    res.status(400).json({ ok: false, error: e.message });
  }
};

// ===============================================
// DELETE /api/historias-clinicas/:id
// ===============================================
exports.eliminarHistoria = async (req, res) => {
  try {
    const db = await connect();
    const col = db.collection("historias_clinicas");
    const oid = oidMaybe(req.params.id);
    if (!oid) return res.status(400).json({ ok: false, error: "ID inválido" });

    const r = await col.deleteOne({ _id: oid });
    if (r.deletedCount === 0) {
      return res.status(404).json({ ok: false, error: "Historia clínica no encontrada" });
    }

    res.json({ ok: true, deleted: r.deletedCount });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
};
