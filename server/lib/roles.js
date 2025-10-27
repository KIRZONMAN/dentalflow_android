// lib/roles.js
const { ObjectId } = require("mongodb");

// Catálogo permitido (coincide con validate.js -> RolEnum)
const ROLE_NAMES = ["Administrador", "Odontologo", "Asistente", "Laboratorista"];


async function normalizeRole(db, payload) {
  const out = { ...payload };

  // Helper: parsea un posible ObjectId sin lanzar
  const parseOid = (v) => {
    try {
      return typeof v === "string" ? new ObjectId(v) : v instanceof ObjectId ? v : null;
    } catch {
      return null;
    }
  };

  // Solo rol_id
  if (out.rol_id && !out.rol) {
    const rid = parseOid(out.rol_id);
    if (rid) {
      const r = await db.collection("roles").findOne({ _id: rid });
      if (r && r.nombre) out.rol = r.nombre;
    }
    return out;
  }

  // Solo rol (nombre)
  if (out.rol && !out.rol_id) {
    // Asegura que el nombre sea uno permitido (si ya pasó Zod, esto es redundante pero seguro)
    if (!ROLE_NAMES.includes(out.rol)) return out;
    const r = await db.collection("roles").findOne({ nombre: out.rol });
    if (r?._id) out.rol_id = r._id;
    return out;
  }

  // Vienen ambos
  if (out.rol && out.rol_id) {
    const rid = parseOid(out.rol_id);
    if (rid) {
      const rById = await db.collection("roles").findOne({ _id: rid });
      if (rById) {
        // Si el nombre no coincide, confiamos en el ID y corregimos el nombre
        if (rById.nombre && rById.nombre !== out.rol) {
          out.rol = rById.nombre;
        }
        return out;
      }
    }
    // Si el ID no existe, intentamos resolver por nombre y corregimos el ID
    if (ROLE_NAMES.includes(out.rol)) {
      const rByName = await db.collection("roles").findOne({ nombre: out.rol });
      if (rByName?._id) out.rol_id = rByName._id;
    }
  }

  return out;
}

/**
 * (Opcional) Semilla de roles si la colección está vacía.
 * Útil en dev o entornos limpios.
 */
async function ensureRolesSeed(db) {
  const col = db.collection("roles");
  const count = await col.estimatedDocumentCount();
  if (count > 0) return;

  const docs = ROLE_NAMES.map((nombre) => ({
    nombre,
    descripcion:
      nombre === "Administrador" ? "Gestión del sistema" :
      nombre === "Odontologo"    ? "Atención clínica"    :
      nombre === "Asistente"     ? "Apoyo a operación"    :
      "Laboratorio",
    permisos: [] // si luego quieres detallar permisos, aquí va
  }));
  await col.insertMany(docs);
}

module.exports = { normalizeRole, ensureRolesSeed, ROLE_NAMES };
