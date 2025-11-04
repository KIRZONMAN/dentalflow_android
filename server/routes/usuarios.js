// routes/usuarios.js
const express = require("express");
const router = express.Router();
const usuariosController = require("../controllers/usuariosController");
const { connect, oidMaybe } = require("../lib/mongo");
const { usuarioSchemaCreate, usuarioSchemaPatch } = require("../lib/validate");
const { normalizeRole } = require("../lib/roles");

// Rutas de usuario

// GET /api/usuarios
// Soporta: ?search=&rol=&estado=&page=&limit=
router.get("/", usuariosController.listarUsuarios); // Devuelve lista resumida (nombre completo, rol string y rol_id)
// GET /api/usuarios/:id
router.get("/:id", usuariosController.obtenerUsuario);
// POST /api/usuarios
// Acepta rol o rol_id (normalizeRole deja ambos consistentes).
router.post("/", usuariosController.crearUsuario); // "especialidad" es OPCIONAL 
// PATCH /api/usuarios/:id
// Permite actualizar parciales, incluido cambio de rol/rol_id.
router.patch("/:id", usuariosController.actualizarUsuario); // Si envían userId en el body, se usa ese filtro en lugar de :id.
// DELETE /api/usuarios/:id
// Hard delete simple (no hay referencia fuerte por ahora).
router.delete("/:id", usuariosController.eliminarUsuario);

module.exports = router;