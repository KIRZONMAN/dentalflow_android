package com.dentalflow.myapplication.data.remote.model

data class OrdenCompra(
    val _id: String,
    val proveedor_id: String,
    val usuario_id: String,
    val estado: String,
    val fecha_expedicion: String,
    val total: Double
)
