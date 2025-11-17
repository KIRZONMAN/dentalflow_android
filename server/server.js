// server.js
const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
dotenv.config();

const apiKeyAuth = require("./middlewares/auth");

// Rutas
const usuarios = require("./routes/usuarios");
const pacientes = require("./routes/pacientes");
const citas = require("./routes/citas");
const historias = require("./routes/historias");
const procedimientos = require("./routes/procedimientos");
const roles = require("./routes/roles");
const ordenesLab = require("./routes/ordenes_laboratorio");

const app = express();

// Middlewares base
app.use(cors());
app.use(express.json());

// Health (público)
app.get("/health", (req, res) => res.json({ ok: true, ts: Date.now() }));

// Todo lo que cuelga de /api exige API Key
//app.use("/api", apiKeyAuth);

// Subrutas protegidas
app.use("/api/usuarios", usuarios);
app.use("/api/pacientes", pacientes);
app.use("/api/citas", citas);
app.use("/api/historias", historias);
app.use("/api/procedimientos", procedimientos);
app.use("/api/roles", roles);
app.use("/api/ordenes-laboratorio", ordenesLab);
app.use("/api/historias-clinicas", require("./routes/historiasClinicas"));

// 404 para cualquier endpoint no encontrado
app.use((req, res) => res.status(404).json({ ok: false, error: "Not found" }));

// Manejador de errores (fallback)
app.use((err, req, res, next) => {
  console.error("[unhandled]", err);
  res.status(500).json({ ok: false, error: "Internal error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`DentalFlow server running on http://localhost:${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  
});
