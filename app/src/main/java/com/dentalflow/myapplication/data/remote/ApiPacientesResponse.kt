package com.dentalflow.myapplication.data.remote

data class ApiPacientesResponse(
    val ok: Boolean,
    val total: Int,
    val page: Int? = null,
    val pageSize: Int? = null,
    val data: List<PacienteDto>
)
