package com.dentalflow.myapplication.Asistente

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.ApiPacientesResponse
import com.dentalflow.myapplication.data.remote.PacienteAdapter
import com.dentalflow.myapplication.data.remote.PacienteItem
import com.dentalflow.myapplication.databinding.ActivityCrearCitaBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ActivityCrearCita : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCitaBinding
    private val client = OkHttpClient()
    private val BASE_URL = "http://10.0.2.2:3000/api"
    private val PACIENTES_URL = "$BASE_URL/pacientes"
    private val API_URL = "$BASE_URL/citas"
    private var fechaSeleccionada: Calendar? = null
    private var searchTimer: Timer? = null
    private var bloqueandoBusqueda = false
    private var pacienteSeleccionado: PacienteItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Spinner de estados
        val estados = listOf("Pendiente","Confirmada")
        binding.spinnerEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        //Odontologos simulados (POR EL MOMENTO)
        val odontologos = listOf("68cf25706f565e081eca7c7d")

        // 🔹 AutoComplete de pacientes
        binding.autoPaciente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (bloqueandoBusqueda) return
                searchTimer?.cancel()

                if (!s.isNullOrBlank() && s.length >= 2) {
                    searchTimer = Timer()
                    searchTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread { buscarPacientes(s.toString()) }
                        }
                    }, 500)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

// 🔹 Al seleccionar paciente
        binding.autoPaciente.setOnItemClickListener { _, _, position, _ ->
            val paciente = binding.autoPaciente.adapter.getItem(position) as PacienteItem
            pacienteSeleccionado = paciente
        }

        binding.spinnerOdontologo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, odontologos)

        // 🔹 Abrir calendario
        binding.etFecha.setOnClickListener { mostrarCalendario() }

        // 🔹 Enviar cita
        binding.btnAgendar.setOnClickListener { enviarCita() }
    }

    private fun buscarPacientes(query: String) {
        val url = "$PACIENTES_URL?q=${query.trim()}"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ActivityCrearCita, "❌ Error al buscar pacientes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@ActivityCrearCita, "⚠️ Error: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val json = response.body?.string()
                    val gson = Gson()
                    val apiResponse = gson.fromJson(json, ApiPacientesResponse::class.java)

                    runOnUiThread {
                        if (apiResponse.ok && apiResponse.data.isNotEmpty()) {
                            val pacientes = apiResponse.data.map {
                                PacienteItem("${it.nombres} ${it.apellidos}", it._id)
                            }
                            val adapter = PacienteAdapter(this@ActivityCrearCita, pacientes)
                            binding.autoPaciente.setAdapter(adapter)
                        }
                    }
                }
            }
        })
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                fechaSeleccionada = cal
                mostrarSelectorHora(cal)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun mostrarSelectorHora(cal: Calendar) {
        val hora = cal.get(Calendar.HOUR_OF_DAY)
        val minuto = cal.get(Calendar.MINUTE)
        val timePicker = TimePickerDialog(this, { _, h, m ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)

            val formato = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            binding.etFecha.setText(formato.format(cal.time))
            fechaSeleccionada = cal
        }, hora, minuto, true)
        timePicker.show()
    }


    private fun enviarCita() {
        val pacienteId = pacienteSeleccionado?.id
        if (pacienteId == null) {
            Toast.makeText(this, "⚠️ Selecciona un paciente válido", Toast.LENGTH_SHORT).show()
            return
        }
        val usuarioId = binding.spinnerOdontologo.selectedItem.toString()
        val motivo = binding.etMotivo.text.toString().trim()
        val estado = binding.spinnerEstado.selectedItem.toString()

        if (fechaSeleccionada == null || motivo.isEmpty()) {
            Toast.makeText(this, "⚠️ Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Convertir fecha al formato ISO requerido por MongoDB
        val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdfIso.timeZone = TimeZone.getTimeZone("UTC")
        val fechaIso = sdfIso.format(fechaSeleccionada!!.time)

        val json = JSONObject().apply {
            put("fecha", fechaIso)
            put("paciente_id", pacienteId)
            put("usuario_id", usuarioId)
            put("motivo", motivo)
            put("estado", estado)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .build()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val respuesta = response.body?.string()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        SweetAlertDialog(this@ActivityCrearCita, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Éxito!")
                            .setContentText("Cita registrada correctamente ✅")
                            .setConfirmText("OK")
                            .setConfirmClickListener { dialog ->
                                dialog.dismissWithAnimation()
                                limpiarCampos()
                                val intent = Intent(this@ActivityCrearCita, ActivityCitas::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    } else {
                        SweetAlertDialog(this@ActivityCrearCita, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Error del servidor: $respuesta ❌")
                            .setConfirmText("Reintentar")
                            .setConfirmClickListener { dialog ->
                                dialog.dismissWithAnimation()
                            }
                            .show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActivityCrearCita, "⚠️ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun limpiarCampos() {
        binding.etFecha.text.clear()
        binding.etMotivo.text.clear()
        binding.autoPaciente.text.clear()
        binding.spinnerEstado.setSelection(0)
        pacienteSeleccionado = null
    }
}
