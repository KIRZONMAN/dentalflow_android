package com.dentalflow.myapplication.data.remote.model

data class HistoriaClinicaDto(
    val _id: String,
    val paciente_id: String,
    val antecedentes_medicos: List<Antecedente> = emptyList(),
    val alergias: List<Alergia> = emptyList(),
    val recetas_medicas: List<Receta> = emptyList(),
    val procedimientos_realizados: List<Procedimiento> = emptyList()
)

data class Antecedente(
    val enfermedad: String?,
    val gravedad: String?
)

data class Alergia(
    val alergia_a: String?,
    val reaccion: String?
)

data class Receta(
    val tipo_orden: String,
    val medicamento: String?,
    val descripcion: String?,
    val fecha_receta: String?
)

data class Procedimiento(
    val tratamiento: String?,
    val fecha: String?,
    val odontologo: String?,
    val resultado: String?
)
