package com.dentalflow.myapplication.data.remote.model

data class OdontologoDto(
    val _id: String,
    val nombres: String,
    val apellidos: String,
    val correo: String?,
    val estado: String,
    val rol: String?,
    val especialidad: List<String>?,
    val rol_id: String
)