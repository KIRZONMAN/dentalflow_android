// lib/validate.js
const { z } = require("zod");

/* ===========================
   Enums y normalizadores
   =========================== */
const EstadoEnum = z.enum(["activo", "inactivo", "suspendido"]);
const EstadoCi = z.string().transform(s => s.trim().toLowerCase()).pipe(EstadoEnum);

const RolEnum = z.enum(["Administrador", "Odontologo", "Asistente", "Laboratorista"]);
const OidStr  = z.string().regex(/^[0-9a-fA-F]{24}$/, "ObjectId inválido");

// Fecha: acepta Date o string parseable y valida que sea Date válido
const Fecha = z.preprocess((v) => (v instanceof Date ? v : new Date(v)), z.date());


const usuarioSchemaCreate = z.object({
  nombres:  z.string().min(1).transform(s => s.trim()),
  apellidos:z.string().min(1).transform(s => s.trim()),
  correo:   z.string().email().transform(s => s.trim().toLowerCase()),
  estado:   EstadoCi.default("activo"),
  rol:      RolEnum,
  rol_id:   OidStr.optional(),
  direccion: z.string().optional().transform(s => s?.trim() || undefined),
  telefono:  z.string().optional().transform(s => s?.trim() || undefined),
  especialidad: z.union([z.string(), z.array(z.string())]).optional(),
  userId: z.string().optional().nullable(),
}).superRefine((val, ctx) => {
  // Solo es obligatoria para Odontólogo
  if (val.rol === "Odontologo") {
    const ok = Array.isArray(val.especialidad)
      ? val.especialidad.length > 0
      : typeof val.especialidad === "string" && val.especialidad.trim() !== "";
    if (!ok) ctx.addIssue({ code: "custom", message: "especialidad es obligatoria para rol Odontologo" });
  }
});

const usuarioSchemaPatch = z.object({
  userId: z.string().optional(),
  nombres:  z.string().min(1).optional(),
  apellidos:z.string().min(1).optional(),
  correo:   z.string().email().optional(),
  estado:   EstadoCi.optional(),
  rol:      RolEnum.optional(),
  rol_id:   OidStr.optional(),
  direccion: z.string().optional(),
  telefono:  z.string().optional(),
  especialidad: z.union([z.string(), z.array(z.string())]).optional(),
}).refine(v => {
  const keys = ["nombres","apellidos","correo","estado","rol","rol_id","direccion","telefono","especialidad"];
  return keys.some(k => v[k] !== undefined);
}, { message: "Nada para actualizar" });

/* ===========================
   PACIENTES
   =========================== */
const pacienteSchemaUpsert = z.object({
  _id:        z.string().min(1).transform(s => s.trim()), // cédula (string) por ahora
  nombres:    z.string().min(1).transform(s => s.trim()),
  apellidos:  z.string().min(1).transform(s => s.trim()),
  edad:       z.number().int().min(0).max(120),
  genero:     z.string().min(1).transform(s => s.trim()),
  telefono:   z.string().optional().transform(s => s?.trim() || undefined),
  direccion:  z.string().optional().transform(s => s?.trim() || undefined),
  correo:     z.string().email().optional().transform(s => s?.toLowerCase() || undefined),
  tipo_sangre:z.string().min(1),
});

function titleCase(s) {
  return s.replace(/\s+/g, " ")
          .trim()
          .toLowerCase()
          .replace(/\b\p{L}/gu, m => m.toUpperCase());
}

function normalizePaciente(p) {
  return {
    ...p,
    nombres:  titleCase(p.nombres),
    apellidos:titleCase(p.apellidos),
    genero:   titleCase(p.genero),
    tipo_sangre: p.tipo_sangre.toUpperCase().replace(/\s+/g, ""),
  };
}

/* ===========================
   CITAS
   =========================== */
const citaProcItem = z.object({
  procedimiento_id: OidStr.optional(),
  nombre:          z.string().min(1),
  costo_unitario:  z.number().nonnegative(),
  cantidad:        z.number().int().min(1).default(1),
});

const citaSchemaCreate = z.object({
  fecha:        Fecha,
  paciente_id:  z.string().min(1),          // por ahora string (cédula)
  usuario_id:   OidStr,
  estado:       z.enum(["Pendiente","Completada","Cancelada","Confirmada"]).optional(),
  motivo:       z.string().optional(),
  // Si viene, al menos 1; si no viene, la ruta puede calcular total=0.
  procedimientos: z.array(citaProcItem).min(1).optional(),
  total:        z.number().nonnegative().optional(),
});

/* ===========================
   HISTORIAS CLÍNICAS
   =========================== */
const historiaProcRealizado = z.object({
  tratamiento: z.string().min(1),
  fecha:       Fecha,
  odontologo:  z.string().optional(),
  resultado:   z.string().optional(),
});

// Variante: un solo procedimiento
const historiaAppendOneSchema = z.object({
  paciente_id: z.string().min(1),
  procedimiento: historiaProcRealizado,
});

// Variante: varios a la vez
const historiaAppendManySchema = z.object({
  paciente_id: z.string().min(1),
  procedimientos_realizados: z.array(historiaProcRealizado).min(1),
});

// Variante flexible (como la ruta actual): uno o varios
const historiaAppendFlexibleSchema = z.object({
  paciente_id: z.string().min(1),
  procedimiento: historiaProcRealizado.optional(),
  procedimientos_realizados: z.array(historiaProcRealizado).optional(),
}).refine(v =>
  v.procedimiento || (Array.isArray(v.procedimientos_realizados) && v.procedimientos_realizados.length > 0),
  { message: "Debe incluir 'procedimiento' o 'procedimientos_realizados'" }
);

/* ===========================
   ÓRDENES DE LABORATORIO
   =========================== */
const ordenLabProducto = z.object({
  tipo_producto:   z.string().min(1),
  especificaciones:z.string().optional(),
  cantidad:        z.number().int().min(1).default(1),
});

const ordenLabCreateSchema = z.object({
  cita_id:      OidStr,
  usuario_id:   OidStr,
  fecha_creacion: Fecha.optional(), // la ruta ya pone new Date() si no viene
  estado:       z.enum(["Pendiente","En producción","Listo para enviar","Entregada","Rechazada"]).default("Pendiente"),
  observaciones:z.any().optional(),
  productos:    z.array(ordenLabProducto).min(1),
});

/* ===========================
   Exports
   =========================== */
module.exports = {
  // usuarios
  usuarioSchemaCreate,
  usuarioSchemaPatch,

  // pacientes
  pacienteSchemaUpsert,
  normalizePaciente,
  titleCase,

  // citas
  citaSchemaCreate,

  // historias clínicas
  historiaAppendOneSchema,
  historiaAppendManySchema,
  historiaAppendFlexibleSchema,

  // órdenes de laboratorio
  ordenLabCreateSchema,
};
