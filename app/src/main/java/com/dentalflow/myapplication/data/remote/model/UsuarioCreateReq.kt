package com.dentalflow.myapplication.data.remote.model

/**
 * Para POST /api/usuarios
 * - 'especialidad' solo obligatoria si rol = "Odontologo"
 */
data class UsuarioCreateReq(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val estado: String = "activo",
    val rol: String,
    val rol_id: String? = null,
    val direccion: String? = null,
    val telefono: String? = null,
    val especialidad: List<String>? = null,
    val userId: String? = null
)