package com.dentalflow.myapplication.data.remote.model

/**
 * Para PATCH /api/usuarios/:id
 * Todos opcionales (el backend valida que venga al menos 1).
 */
data class UsuarioPatchReq(
    val userId: String? = null,
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