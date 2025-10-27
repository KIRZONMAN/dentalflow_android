package com.dentalflow.myapplication.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.dentalflow.myapplication.data.remote.model.ApiResponse
import com.dentalflow.myapplication.data.remote.model.UsuarioDto
import com.dentalflow.myapplication.data.remote.model.UsuarioCreateReq
import com.dentalflow.myapplication.data.remote.model.UsuarioPatchReq
import com.dentalflow.myapplication.data.remote.model.PacienteDto


object Api {

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val client: OkHttpClient = Http.client
    private val BASE = Http.baseUrl // ej: http://10.0.2.2:3000/api

    private fun reqBuilder(url: String): Request.Builder =
        Request.Builder().url(url) // x-api-key lo agrega el interceptor

    private suspend fun <T> call(request: Request, typeToken: TypeToken<T>): T =
        suspendCancellableCoroutine { cont ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isCancelled) cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val bodyStr = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            cont.resumeWithException(IOException("HTTP ${it.code}: $bodyStr"))
                            return
                        }
                        try {
                            val obj: T = gson.fromJson(bodyStr, typeToken.type)
                            cont.resume(obj)
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }
                }
            })
        }

    // -------- Usuarios --------

    suspend fun getUsuarios(search: String): ApiResponse<List<UsuarioDto>> {
        val url = "$BASE/usuarios"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("search", search)
            .build()
            .toString()

        val req = reqBuilder(url).get().build()
        val tt = object : TypeToken<ApiResponse<List<UsuarioDto>>>(){}
        return call(req, tt)
    }

    suspend fun createUsuario(body: UsuarioCreateReq): ApiResponse<Any> {
        val url = "$BASE/usuarios"
        val json = gson.toJson(body).toRequestBody(JSON)
        val req = reqBuilder(url).post(json).build()
        val tt = object : TypeToken<ApiResponse<Any>>() {}
        return call(req, tt)
    }

    suspend fun patchUsuario(id: String, body: UsuarioPatchReq): ApiResponse<Any> {
        val url = "$BASE/usuarios/$id"
        val json = gson.toJson(body).toRequestBody(JSON)
        val req = reqBuilder(url).patch(json).build()
        val tt = object : TypeToken<ApiResponse<Any>>() {}
        return call(req, tt)
    }

    suspend fun deleteUsuario(id: String): ApiResponse<Any> {
        val url = "$BASE/usuarios/$id"
        val req = reqBuilder(url).delete().build()
        val tt = object : TypeToken<ApiResponse<Any>>() {}
        return call(req, tt)
    }

    // -------- Pacientes --------

    suspend fun upsertPaciente(body: PacienteDto): ApiResponse<Any> {
        val url = "$BASE/pacientes"
        val json = gson.toJson(body).toRequestBody(JSON)
        val req = reqBuilder(url).post(json).build()
        val tt = object : TypeToken<ApiResponse<Any>>() {}
        return call(req, tt)
    }
}
