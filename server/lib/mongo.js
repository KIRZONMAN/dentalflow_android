// lib/mongo.js
const { MongoClient, ObjectId } = require("mongodb");
const dotenv = require("dotenv");
const { ensureRolesSeed } = require("./roles");
dotenv.config();

let client;
let db;

/** Intenta crear un índice y NO falla si ya existe con otro nombre/opciones */
async function createIndexSafe(col, keys, options = {}) {
  try {
    return await col.createIndex(keys, { ...options });
  } catch (e) {
    const msg = String(e && e.message || "");
    // Conflictos típicos cuando ya existe con otro nombre/opción
    if (
      e.codeName === "IndexOptionsConflict" ||
      e.codeName === "IndexKeySpecsConflict" ||
      msg.includes("already exists")
    ) {
      return null; // ignoramos
    }
    throw e;
  }
}

async function ensureBaseIndexes(db) {
  // USUARIOS
  await createIndexSafe(db.collection("usuarios"), { correo: 1 }, { name: "uq_usuarios_correo", unique: true });
  await createIndexSafe(db.collection("usuarios"), { userId: 1 }, { name: "ix_usuarios_userId" });
  await createIndexSafe(db.collection("usuarios"), { rol: 1 }, { name: "ix_usuarios_rol" });
  await createIndexSafe(db.collection("usuarios"), { rol_id: 1 }, { name: "ix_usuarios_rol_id" });

  // PACIENTES
  await createIndexSafe(db.collection("pacientes"), { apellidos: 1, nombres: 1 }, { name: "ix_pacientes_nombre" });
  await createIndexSafe(db.collection("pacientes"), { correo: 1 }, { name: "ix_pacientes_correo" });
  // (_id) ya viene indexado por defecto

  // CITAS
  await createIndexSafe(db.collection("citas"), { paciente_id: 1, fecha: -1 }, { name: "ix_citas_paciente_fecha" });
  await createIndexSafe(db.collection("citas"), { usuario_id: 1, fecha: -1 }, { name: "ix_citas_usuario_fecha" });

  // HISTORIAS CLÍNICAS
  await createIndexSafe(db.collection("historias_clinicas"), { paciente_id: 1 }, { name: "uq_historia_por_paciente", unique: true });

  // PROCEDIMIENTOS
  await createIndexSafe(db.collection("procedimientos"), { tipo_procedimiento: 1 }, { name: "uq_procedimiento_tipo", unique: true });

  // INSUMOS
  await createIndexSafe(db.collection("insumos"), { nombre: 1 }, { name: "uq_insumo_nombre", unique: true });

  // ÓRDENES DE COMPRA
  await createIndexSafe(db.collection("ordenes_compras"), { fecha_expedicion: -1 }, { name: "ix_oc_fecha" });
  await createIndexSafe(db.collection("ordenes_compras"), { proveedor_id: 1, fecha_expedicion: -1 }, { name: "ix_oc_proveedor_fecha" });

  // ÓRDENES DE LABORATORIO
  await createIndexSafe(db.collection("ordenes_laboratorio"), { fecha_creacion: -1 }, { name: "ix_ol_fecha" });
  await createIndexSafe(db.collection("ordenes_laboratorio"), { cita_id: 1 }, { name: "ix_ol_cita" });

  // PROVEEDORES (si la usas)
  await createIndexSafe(db.collection("proveedores"), { nombre: 1 }, { name: "uq_proveedor_nombre", unique: true });

  // PROVEEDORES_INSUMOS (si la usas)
  await createIndexSafe(db.collection("proveedores_insumos"), { proveedor_id: 1, insumo_id: 1 }, { name: "uq_prov_insumo", unique: true });
}

async function connect() {
  if (db) return db;

  const uri = process.env.MONGODB_URI;
  if (!uri) throw new Error("MONGODB_URI missing in .env");

  client = new MongoClient(uri);
  await client.connect();

  db = client.db(process.env.DB_NAME || "DBDentalFlow");

  // Índices y semilla de roles (idempotente)
  await ensureBaseIndexes(db);
  await ensureRolesSeed(db);

  return db;
}

function oidMaybe(id) {
  try {
    return new ObjectId(id);
  } catch {
    return null;
  }
}

module.exports = { connect, oidMaybe, ObjectId };
