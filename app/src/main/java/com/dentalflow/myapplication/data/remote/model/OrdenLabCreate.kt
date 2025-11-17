package com.dentalflow.myapplication.data.remote.model

data class OrdenLabCreate(
    val cita_id: String,
    val usuario_id: String,
    val fecha_creacion: String?,     // opcional → puede ser null
    val estado: String = "Pendiente",
    val observaciones: String?,
    val productos: List<ProductoLab>
)