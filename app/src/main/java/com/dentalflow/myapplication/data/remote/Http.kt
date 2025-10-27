package com.dentalflow.myapplication.data.remote

import com.dentalflow.myapplication.BuildConfig //error en esta linea
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object Http {

    private val apiKeyInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("x-api-key", BuildConfig.API_KEY) //error en esta linea
            .build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        // Nivel BODY en debug; INFO en prod
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY //error en esta linea
        else HttpLoggingInterceptor.Level.BASIC
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(logging)
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // BASE_URL viene del flavor y debe incluir /api
    val baseUrl: String by lazy { BuildConfig.BASE_URL.trimEnd('/') } //error en esta linea
}
