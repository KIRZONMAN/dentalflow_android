const express = require("express");
const router = express.Router();
const historiasController = require("../controllers/historiasClinicasController");

router.get("/", historiasController.listarHistorias);
router.get("/:id", historiasController.obtenerHistoria);
router.post("/", historiasController.crearHistoria);
router.patch("/:id", historiasController.actualizarHistoria);
router.delete("/:id", historiasController.eliminarHistoria);

module.exports = router;
