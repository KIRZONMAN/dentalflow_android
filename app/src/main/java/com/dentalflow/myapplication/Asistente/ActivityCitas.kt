package com.dentalflow.myapplication.Asistente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.data.remote.CitaAdapter
import com.dentalflow.myapplication.data.remote.model.Cita
import com.dentalflow.myapplication.databinding.ActivityCitasBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ActivityCitas : AppCompatActivity() {
    private var sseCall: Call? = null
    private lateinit var binding: ActivityCitasBinding
    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api"
    private val API_CITAS = "http://10.0.2.2:3000/api/citas"
    private val API_USUARIOS = BASE_URL + "/usuarios"
    private lateinit var adapter: CitaAdapter
    private val listaCitas = mutableListOf<Cita>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CitaAdapter(
            listaCitas,
            onEditClick = { cita ->
                // EDITAR CITA
                val intent = Intent(this, EditCitas::class.java)
                intent.putExtra("CITA_ID", cita._id)
                startActivity(intent)
            },
            onAceptarClick = { cita ->
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Aceptar cita")
                    .setContentText("¿Deseas aceptar la cita de ${cita.paciente_nombre}?")
                    .setConfirmText("Sí")
                    .setCancelText("No")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        aceptarCita(cita)
                    }
                    .show()
            },
            onCancelarClick = { cita ->
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Cancelar cita")
                    .setContentText("¿Estás seguro de cancelar esta cita?")
                    .setConfirmText("Confirmar")
                    .setCancelText("Volver")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        cancelarCita(cita)
                    }
                    .show()
            }
        )
        binding.recyclerCitas.adapter = adapter
        binding.recyclerCitas.layoutManager = LinearLayoutManager(this)

        //Ir a Agendar Cita
        binding.btnCrear.setOnClickListener {
            val irAgendarCita = Intent(this, ActivityCrearCita::class.java)
            startActivity(irAgendarCita)
        }

        obtenerCitas()
        escucharCitasStream()
    }
    private fun aceptarCita(cita: Cita) {
        val url = "$API_CITAS/${cita._id}"
        val json = """
        {
            "estado": "Confirmada"
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .patch(body)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("No se pudo aceptar la cita")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Error en el servidor: $responseBody")
                            .show()
                    }
                    return
                }

                runOnUiThread {
                    SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Cita confirmada")
                        .setContentText("La cita fue cambiada a Confirmada")
                        .show()
                }
            }
        })
    }

    private fun cancelarCita(cita: Cita) {
        val url = "$API_CITAS/${cita._id}"
        val json = """
        {
            "estado": "Cancelada"
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .patch(body)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("No se pudo cancelar la cita")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Error en el servidor: $responseBody")
                            .show()
                    }
                    return
                }

                runOnUiThread {
                    SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Cita cancelada")
                        .setContentText("La cita fue cambiada a Cancelada")
                        .show()
                }
            }
        })
    }

    //Obtener citas
    private fun obtenerCitas() {
        val request = Request.Builder()
            .url(API_CITAS)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("⚠️ Error de conexión")
                        .setContentText(e.message ?: "No se pudo conectar al servidor")
                        .setConfirmText("Aceptar")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("❌ Error")
                            .setContentText("Código: ${response.code}")
                            .setConfirmText("Cerrar")
                            .show()
                        return@runOnUiThread
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val dataArray = json.getJSONArray("data")
                    val citasList = mutableListOf<Cita>()

                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        val cita = Cita(
                            _id = obj.getString("_id"),
                            fecha = obj.getString("fecha"),
                            estado = obj.optString("estado", "Pendiente"),
                            motivo = obj.optString("motivo", ""),
                            total = obj.optDouble("total", 0.0),
                            paciente_id = obj.optString("paciente_id", ""),
                            paciente_nombre = obj.optString("paciente_nombre", "Desconocido"),
                            usuario_id = obj.optString("usuario_id", "")
                        )
                        citasList.add(cita)
                    }

                    // Obtener los nombres de los usuarios y actualizar la lista
                    obtenerNombresUsuarios(citasList)

                } catch (e: Exception) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityCitas, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("⚠️ Error parseando datos")
                            .setContentText(e.message ?: "Ocurrió un error al procesar la respuesta")
                            .setConfirmText("Aceptar")
                            .show()}
                }
            }
        })
    }
    //CAMBIOS EN LA DB
    private fun escucharCitasStream() {

        val request = Request.Builder()
            .url("$API_CITAS/stream")
            .addHeader("Accept", "text/event-stream")
            .build()

        sseCall = client.newCall(request)

        sseCall?.enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                // 🔄 Esperar 2 segundos y reconectar automáticamente
                binding.recyclerCitas.postDelayed({
                    escucharCitasStream()
                }, 2000)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return

                val source = response.body?.source() ?: return

                try {
                    while (!call.isCanceled()) {
                        val line = source.readUtf8Line() ?: continue

                        if (line.startsWith("data: ")) {
                            val rawJson = line.removePrefix("data: ").trim()

                            val change = JSONObject(rawJson)
                            val operation = change.getString("operationType")
                            val fullDoc = change.optJSONObject("fullDocument")

                            runOnUiThread {

                                when (operation) {

                                    "insert" -> {
                                        if (fullDoc != null) {
                                            val nueva = parsearCita(fullDoc)
                                            listaCitas.add(0, nueva)
                                            adapter.notifyItemInserted(0)
                                        }
                                    }

                                    "update", "replace" -> {
                                        if (fullDoc != null) {
                                            val actual = parsearCita(fullDoc)
                                            val index = listaCitas.indexOfFirst { it._id == actual._id }
                                            if (index != -1) {
                                                val anterior = listaCitas[index]
                                                val actual = parsearCita(fullDoc)
                                                val merged = actual.copy(
                                                    paciente_nombre =
                                                        if (actual.paciente_nombre.isNullOrBlank())
                                                            anterior.paciente_nombre
                                                        else
                                                            actual.paciente_nombre
                                                )

                                                listaCitas[index] = merged
                                                adapter.notifyItemChanged(index)
                                            }
                                        }
                                    }

                                    "delete" -> {
                                        val id = change
                                            .getJSONObject("documentKey")
                                            .getString("_id")

                                        val index = listaCitas.indexOfFirst { it._id == id }

                                        if (index != -1) {
                                            listaCitas.removeAt(index)
                                            adapter.notifyItemRemoved(index)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Si falla, reconectar
                    binding.recyclerCitas.postDelayed({
                        escucharCitasStream()
                    }, 2000)
                }
            }
        })
    }
    private fun parsearCita(obj: JSONObject): Cita {

        val id = if (obj.optJSONObject("_id") != null) {
            obj.getJSONObject("_id").getString("\$oid")
        } else {
            obj.optString("_id")
        }

        val fecha = when {
            obj.optJSONObject("fecha") != null ->
                java.util.Date(obj.getJSONObject("fecha").getLong("\$date")).toString()
            else ->
                obj.optString("fecha")
        }

        return Cita(
            _id = id,
            fecha = fecha,
            estado = obj.optString("estado", "Pendiente"),
            motivo = obj.optString("motivo", ""),
            total = obj.optDouble("total", 0.0),
            paciente_id = obj.optString("paciente_id", ""),
            paciente_nombre = obj.optString("paciente_nombre", ""),
            usuario_id = obj.optString("usuario_id", ""),
            usuario_nombre = null
        )
    }

    //Obtener usuarios por ID
    private fun obtenerNombresUsuarios(citas: MutableList<Cita>) {
        val citasActualizadas = mutableListOf<Cita>()
        var pendientes = citas.size

        for (cita in citas) {
            val request = Request.Builder()
                .url("$API_USUARIOS/${cita.usuario_id}")
                .get()
                .build()

            // Por si ocurre un error de conexión o la API no responde
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cita.usuario_nombre = "Desconocido"
                    verificarFinal()
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        try {
                            val json = JSONObject(body)
                            val data = json.getJSONObject("data")
                            val nombre = data.optString("nombres", "")
                            val apellido = data.optString("apellidos", "")
                            cita.usuario_nombre = "$nombre $apellido".trim()
                        } catch (e: Exception) {
                            cita.usuario_nombre = "Error al parsear"
                        }
                    } else {
                        cita.usuario_nombre = "No encontrado"
                    }
                    verificarFinal()
                }
               //Actualizar info de citas
               fun verificarFinal() {
                   synchronized(this@ActivityCitas) {
                       citasActualizadas.add(cita)
                       pendientes--
                       if (pendientes == 0) {
                           runOnUiThread {
                               listaCitas.clear()
                               listaCitas.addAll(citasActualizadas)
                               adapter.updateData(listaCitas)
                           }}
                   }
               }

            })
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        sseCall?.cancel()
    }
}
