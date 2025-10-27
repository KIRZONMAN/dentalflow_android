package com.dentalflow.myapplication.data.remote.model

/**
 * Proyección típica del GET /api/usuarios que hiciste:
 *  { _id, userId, nombre, correo, estado, rol }
 */
data class UsuarioDto(
    val _id: String? = null,
    val userId: String? = null,
    val nombre: String? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val correo: String? = null,
    val estado: String? = null,
    val rol: String? = null,
    val rol_id: String? = null,
    val direccion: String? = null,
    val telefono: String? = null,
    val especialidad: List<String>? = null
)