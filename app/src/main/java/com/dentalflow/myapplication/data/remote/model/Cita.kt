package com.dentalflow.myapplication.data.remote.model

data class Cita(
    val _id: String,
    val fecha: String,
    val paciente_nombre: String?,
    val estado: String,
    val motivo: String?,
    val total: Double?,
    val paciente_id: String,
    var usuario_id: String,
    var usuario_nombre: String? = null
) {
    override fun toString(): String = "$paciente_nombre - $fecha"
}
