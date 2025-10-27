package com.dentalflow.myapplication.data.remote

data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val id: String? = null,
    val modified: Int? = null,
    val deleted: Int? = null,
    val upserted: Boolean? = null,
    val error: String? = null
)

data class UsuarioDto(
    val _id: String,
    val userId: String? = null,
    val nombre: String,
    val correo: String,
    val estado: String? = null,
    val rol: String? = null
)

data class UsuarioCreateReq(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val estado: String,
    val rol: String,
    val direccion: String? = null,
    val telefono: String? = null,
    val especialidad: Any? = null // String o List<String>
)

data class UsuarioPatchReq(
    val userId: String? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val correo: String? = null,
    val estado: String? = null,
    val rol: String? = null,
    val direccion: String? = null,
    val telefono: String? = null,
    val especialidad: Any? = null
)

data class PacienteDto(
    val _id: String,
    val nombres: String,
    val apellidos: String,
    val edad: Int,
    val genero: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val correo: String? = null,
    val tipo_sangre: String
)
