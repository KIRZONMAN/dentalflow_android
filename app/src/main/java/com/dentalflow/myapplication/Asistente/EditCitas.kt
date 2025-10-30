package com.dentalflow.myapplication.Asistente

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.databinding.ActivityEditCitasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class EditCitas : AppCompatActivity() {

    private lateinit var binding: ActivityEditCitasBinding
    private val client = OkHttpClient()
    private val API_URL = "http://10.0.2.2:3000/api/citas"

    private var fechaSeleccionada: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar spinners como antes...
        val estados = listOf("Pendiente", "Confirmada", "Cancelada", "Completada")
        binding.spinnerEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        // 🔹 Cargar datos recibidos
        val citaId = intent.getStringExtra("CITA_ID")
        val fecha = intent.getStringExtra("FECHA")
        val motivo = intent.getStringExtra("MOTIVO")
        val estado = intent.getStringExtra("ESTADO")
        val pacienteId = intent.getStringExtra("PACIENTE_ID")
        val usuarioId = intent.getStringExtra("USUARIO_ID")

// 🔹 Simulados (en tu versión final estos vendrán del backend)
        val pacientes = listOf("1002003001", "1002003001")
        val odontologos = listOf(
            "68cf257a6f565e081eca7c7f",
            "68cf257a6f565e081eca7c7f"
        )

        binding.spinnerPaciente.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pacientes)
        binding.spinnerOdontologo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, odontologos)
// 🔹 Mostrar en los campos
        binding.etFecha.setText(fecha)
        binding.etMotivo.setText(motivo)
        binding.spinnerEstado.setSelection(
            (binding.spinnerEstado.adapter as ArrayAdapter<String>).getPosition(estado)
        )
        binding.spinnerPaciente.setSelection(
            (binding.spinnerPaciente.adapter as ArrayAdapter<String>).getPosition(pacienteId)
        )
        binding.spinnerOdontologo.setSelection(
            (binding.spinnerOdontologo.adapter as ArrayAdapter<String>).getPosition(usuarioId)
        )
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

        // 🔹 Verificar si es actualización o creación
        val citaId = intent.getStringExtra("CITA_ID") // si viene del intent, es edición
        val urlFinal = if (citaId != null) "$API_URL/$citaId" else API_URL

        val requestBuilder = Request.Builder()
            .url(urlFinal)
            .header("Content-Type", "application/json")

        val request = if (citaId != null) {
            // 🔹 PATCH para actualizar
            requestBuilder.patch(body).build()
        } else {
            // 🔹 POST para crear
            requestBuilder.post(body).build()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val respuesta = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val mensaje = if (citaId != null)
                            "Cita actualizada correctamente ✅"
                        else
                            "Cita registrada correctamente ✅"

                        SweetAlertDialog(this@EditCitas, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Éxito!")
                            .setContentText(mensaje)
                            .setConfirmText("OK")
                            .setConfirmClickListener { dialog ->
                                dialog.dismissWithAnimation()
                                limpiarCampos()
                                val intent = Intent(this@EditCitas, ActivityCitas::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    } else {
                        SweetAlertDialog(this@EditCitas, SweetAlertDialog.ERROR_TYPE)
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
                    Toast.makeText(this@EditCitas, "⚠️ Error: ${e.message}", Toast.LENGTH_LONG).show()
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
