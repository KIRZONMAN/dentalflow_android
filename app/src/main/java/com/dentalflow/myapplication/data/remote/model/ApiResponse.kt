package com.dentalflow.myapplication.data.remote.model

/**
 * Respuesta genérica del backend.
 * Nota: el backend a veces devuelve { ok, data }, y otras { ok, id } / { ok, modified } / etc.
 * Por eso dejamos campos opcionales prácticos.
 */
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val id: String? = null,
    val error: String? = null,
    val total: Int? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
    val upserted: Boolean? = null,
    val modified: Int? = null,
    val deleted: Boolean? = null,
    val softDeleted: Boolean? = null
)
