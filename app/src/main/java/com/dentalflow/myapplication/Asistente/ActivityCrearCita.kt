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
import com.dentalflow.myapplication.databinding.ActivityCrearCitaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ActivityCrearCita : AppCompatActivity() {

    private lateinit var binding: ActivityCrearCitaBinding
    private val client = OkHttpClient()
    private val API_URL = "http://10.0.2.2:3000/api/citas"

    private var fechaSeleccionada: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Spinner de estados
        val estados = listOf("Pendiente", "Confirmada", "Cancelada", "Completada")
        binding.spinnerEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        // 🔹 Simulados (en tu versión final estos vendrán del backend)
        val pacientes = listOf("1234567890", "9876543210")
        val odontologos = listOf( // Estos SÍ deben ser ObjectId válidos
            "671f2c9dc931fa2786f8a84b",
            "671f2c9dc931fa2786f8a84c"
        )

        binding.spinnerPaciente.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pacientes)
        binding.spinnerOdontologo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, odontologos)

        // 🔹 Abrir calendario
        binding.etFecha.setOnClickListener { mostrarCalendario() }

        // 🔹 Enviar cita
        binding.btnAgendar.setOnClickListener { enviarCita() }
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                // Guardamos fecha base
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
        val pacienteId = binding.spinnerPaciente.selectedItem.toString()
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
        binding.spinnerEstado.setSelection(0)
    }
}
