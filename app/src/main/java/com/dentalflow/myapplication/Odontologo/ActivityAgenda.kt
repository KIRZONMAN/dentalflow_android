package com.dentalflow.myapplication.Odontologo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.data.remote.PacienteAdapter
import com.dentalflow.myapplication.data.remote.model.PacienteDto
import com.dentalflow.myapplication.databinding.ActivityAgendaBinding
import okhttp3.*
import org.json.JSONObject
import androidx.core.widget.addTextChangedListener
import com.dentalflow.myapplication.data.remote.PacientesAdapter
import java.io.IOException

class ActivityAgenda : AppCompatActivity() {

    private lateinit var binding: ActivityAgendaBinding
    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api"
    private val API_PACIENTES = "$BASE_URL/pacientes"
    private lateinit var adapter: PacientesAdapter
    private val listaPacientes = mutableListOf<PacienteDto>()
    private val handler = Handler(Looper.getMainLooper())
    private var ultimaListaJson = ""

    // Refrescar cada 5 segundos
    private val refrescarRunnable = object : Runnable {
        override fun run() {
            obtenerPacientes()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar RecyclerView
        adapter = PacientesAdapter(listaPacientes) { paciente ->
            val intent = Intent(this, ActivityVerHistoria::class.java)
            intent.putExtra("PACIENTE_ID", paciente._id)
            intent.putExtra("PACIENTE_NOMBRE", "${paciente.nombres} ${paciente.apellidos}")
            startActivity(intent)
        }
        binding.rvPacientes.adapter = adapter
        binding.rvPacientes.layoutManager = LinearLayoutManager(this)

        // Buscar paciente por nombre o cédula
        binding.etBuscar.addTextChangedListener { texto ->
            filtrarPacientes(texto.toString())
        }

        // Primera carga
        obtenerPacientes()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refrescarRunnable) // iniciar listener
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refrescarRunnable) // detener listener
    }

    private fun filtrarPacientes(query: String) {
        val filtrados = listaPacientes.filter {
            it.nombres.contains(query, ignoreCase = true) ||
                    it.apellidos.contains(query, ignoreCase = true) ||
                    it._id.contains(query)
        }
        adapter.actualizarLista(filtrados)
    }

    private fun obtenerPacientes() {
        val request = Request.Builder()
            .url(API_PACIENTES)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityAgenda, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("⚠️ Error de conexión")
                        .setContentText(e.message ?: "No se pudo conectar al servidor")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return

                try {
                    if (body == ultimaListaJson) return // evita refrescar si no hay cambios
                    ultimaListaJson = body

                    val json = JSONObject(body)
                    val dataArray = json.getJSONArray("data")
                    val listaTemp = mutableListOf<PacienteDto>()

                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        val p = PacienteDto(
                            _id = obj.getString("_id"),
                            nombres = obj.optString("nombres", ""),
                            apellidos = obj.optString("apellidos", ""),
                            edad = obj.optInt("edad", 0),
                            genero = obj.optString("genero", ""),
                            telefono = obj.optString("telefono", ""),
                            direccion = obj.optString("direccion", ""),
                            correo = obj.optString("correo", ""),
                            tipo_sangre = obj.optString("tipo_sangre", "")
                        )
                        listaTemp.add(p)
                    }

                    runOnUiThread {
                        listaPacientes.clear()
                        listaPacientes.addAll(listaTemp)
                        adapter.actualizarLista(listaPacientes)
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityAgenda, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("⚠️ Error parseando datos")
                            .setContentText(e.message ?: "Error al procesar respuesta")
                            .show()
                    }
                }
            }
        })
    }
}
