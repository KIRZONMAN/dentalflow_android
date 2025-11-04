package com.dentalflow.myapplication.data.remote

import com.dentalflow.myapplication.data.remote.model.OdontologoDto

data class ApiOdontologoResponse(
    val ok: Boolean,
    val total: Int,
    val page: Int? = null,
    val pageSize: Int? = null,
    val data: List<OdontologoDto>
)
