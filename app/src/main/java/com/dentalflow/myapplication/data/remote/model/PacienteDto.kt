package com.dentalflow.myapplication.data.remote.model

/**
 * Para POST /api/pacientes (upsert por cédula _id)
 */
data class PacienteDto(
    val _id: String,            // cédula (string)
    val nombres: String,
    val apellidos: String,
    val edad: Int,
    val genero: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val correo: String? = null,
    val tipo_sangre: String
)