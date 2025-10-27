package com.dentalflow.myapplication.Administrador

data class UsuarioUi(
    val id: String,
    val userId: String?,
    val nombre: String,   // "Nombres Apellidos"
    val correo: String,
    val estado: String?,
    val rol: String?
)
