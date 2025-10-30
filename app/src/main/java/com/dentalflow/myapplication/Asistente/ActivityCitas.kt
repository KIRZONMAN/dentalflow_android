package com.dentalflow.myapplication.Asistente

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dentalflow.myapplication.data.remote.CitaAdapter
import com.dentalflow.myapplication.data.remote.model.Cita
import com.dentalflow.myapplication.databinding.ActivityCitasBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ActivityCitas : AppCompatActivity() {

    private lateinit var binding: ActivityCitasBinding
    private val client = OkHttpClient()
    private val API_CITAS = "http://10.0.2.2:3000/api/citas"
    private val API_USUARIOS = "http://10.0.2.2:3000/api/usuarios"
    private lateinit var adapter: CitaAdapter
    private val listaCitas = mutableListOf<Cita>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CitaAdapter(listaCitas) { citaSeleccionada ->
            val intent = Intent(this, EditCitas::class.java)
            intent.putExtra("CITA_ID", citaSeleccionada._id)
            intent.putExtra("FECHA", citaSeleccionada.fecha)
            intent.putExtra("MOTIVO", citaSeleccionada.motivo)
            intent.putExtra("ESTADO", citaSeleccionada.estado)
            intent.putExtra("PACIENTE_ID", citaSeleccionada.paciente_id)
            intent.putExtra("USUARIO_ID", citaSeleccionada.usuario_id)
            startActivity(intent)
        }
        binding.recyclerCitas.adapter = adapter
        binding.recyclerCitas.layoutManager = LinearLayoutManager(this)


        binding.btnCrear.setOnClickListener {
            val irAgendarCita = Intent(this, ActivityCrearCita::class.java)
            startActivity(irAgendarCita)
        }

        obtenerCitas()
    }

    private fun obtenerCitas() {
        val request = Request.Builder()
            .url(API_CITAS)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ActivityCitas, "⚠️ Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    runOnUiThread {
                        Toast.makeText(this@ActivityCitas, "❌ Error: ${response.code}", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(this@ActivityCitas, "⚠️ Error parseando datos: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    /** Llama el endpoint para obtener mediante ID los usuarios*/
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
                    cita.usuario_id = "Desconocido"
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
                            cita.usuario_id = "$nombre $apellido".trim()

                            cita.usuario_id = "$nombre $apellido"
                        } catch (e: Exception) {
                            cita.usuario_id = "Error al parsear"
                        }
                    } else {
                        cita.usuario_id = "No encontrado"
                    }
                    verificarFinal()
                }

                /** Método para actualizar*/
                fun verificarFinal() {
                    synchronized(this@ActivityCitas) {
                        citasActualizadas.add(cita)
                        pendientes--
                        if (pendientes == 0) {
                            runOnUiThread {
                                adapter.updateData(citasActualizadas)
                            }
                        }
                    }
                }
            })
        }
    }
}
