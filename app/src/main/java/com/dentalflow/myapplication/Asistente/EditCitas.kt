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
import com.dentalflow.myapplication.data.remote.ApiOdontologoResponse
import com.dentalflow.myapplication.data.remote.ApiPacientesResponse
import com.dentalflow.myapplication.data.remote.OdontologoItem
import com.dentalflow.myapplication.data.remote.PacienteAdapter
import com.dentalflow.myapplication.data.remote.PacienteItem
import com.dentalflow.myapplication.databinding.ActivityEditCitasBinding
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

class EditCitas : AppCompatActivity() {
    private var bloqueandoBusqueda = false

    private lateinit var binding: ActivityEditCitasBinding
    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api"
    private val PACIENTES_URL = "$BASE_URL/pacientes"
    private val API_URL = "$BASE_URL/citas"
    private val API_USUARIOS = "$BASE_URL/usuarios"
    private var searchTimerOdontologo: Timer? = null
    private var searchTimerPacientes: Timer? = null
    private var odontologoSeleccionado: OdontologoItem? = null
    private var fechaSeleccionada: Calendar? = null
    private var pacienteSeleccionado: PacienteItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCitasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val citaId = intent.getStringExtra("CITA_ID")

        if (citaId != null) cargarCitaPorId(citaId)

        binding.autoPaciente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (bloqueandoBusqueda) return
                searchTimerPacientes?.cancel()

                if (!s.isNullOrBlank() && s.length >= 2) {
                    searchTimerPacientes = Timer()
                    searchTimerPacientes!!.schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                                buscarPacientes(s.toString())
                            }
                        }
                    }, 500)
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        // Detectar selección de paciente
        binding.autoPaciente.setOnItemClickListener { _, _, position, _ ->
            val paciente = binding.autoPaciente.adapter.getItem(position) as PacienteItem
            pacienteSeleccionado = paciente
        }
        // 🔹 AutoComplete de ODONTÓLOGOS
        binding.autoOdontologo.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (bloqueandoBusqueda) return
                searchTimerOdontologo?.cancel()

                if (!s.isNullOrBlank() && s.length >= 2) {
                    searchTimerOdontologo = Timer()
                    val query = s.toString().trim()
                    searchTimerOdontologo!!.schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread { buscarOdontologos(query) }
                        }
                    }, 500)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        binding.autoOdontologo.setOnItemClickListener { _, _, position, _ ->
            val odontologo = binding.autoOdontologo.adapter.getItem(position) as OdontologoItem
            odontologoSeleccionado = odontologo
        }

        // Abrir calendario
        binding.etFecha.setOnClickListener { mostrarCalendario() }

        // Guardar cita (PATCH)
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
                    Toast.makeText(this@EditCitas, "❌ Error al buscar pacientes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@EditCitas, "⚠️ Error: ${response.code}", Toast.LENGTH_SHORT).show()
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

                            val adapter = PacienteAdapter(this@EditCitas, pacientes)
                            binding.autoPaciente.setAdapter(adapter)
                        }
                    }
                }
            }
        })
    }

    //Cargar todos los odontólogos como sugerencia
    private fun buscarOdontologos(query: String) {
        val url = "$API_USUARIOS?rol=Odontologo&search=${query.trim()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditCitas, "❌ Error al buscar odontólogos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@EditCitas, "⚠️ Error: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val json = response.body?.string()
                    val gson = Gson()
                    val apiResponse = gson.fromJson(json, ApiOdontologoResponse::class.java)

                    runOnUiThread {
                        if (apiResponse.ok && apiResponse.data.isNotEmpty()) {
                            // 🔸 Mostrar coincidencias
                            val odontologos = apiResponse.data.map {
                                OdontologoItem("${it.nombres} ${it.apellidos}", it._id)
                            }

                            val adapter = ArrayAdapter(
                                this@EditCitas,
                                android.R.layout.simple_dropdown_item_1line,
                                odontologos
                            )
                            binding.autoOdontologo.setAdapter(adapter)
                            binding.autoOdontologo.showDropDown()
                        } else {
                            // 🔸 Si no hay resultados → cargar todos los odontólogos disponibles
                            cargarTodosLosOdontologos()
                        }
                    }
                }
            }
        })
    }
    private fun cargarTodosLosOdontologos() {
        val url = "$API_USUARIOS?rol=Odontologo"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditCitas, "❌ Error al cargar odontólogos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return

                    val json = response.body?.string()
                    val gson = Gson()
                    val apiResponse = gson.fromJson(json, ApiOdontologoResponse::class.java)

                    runOnUiThread {
                        if (apiResponse.ok && apiResponse.data.isNotEmpty()) {
                            val odontologos = apiResponse.data.map {
                                OdontologoItem("${it.nombres} ${it.apellidos}", it._id)
                            }

                            val adapter = ArrayAdapter(
                                this@EditCitas,
                                android.R.layout.simple_dropdown_item_1line,
                                odontologos
                            )
                            binding.autoOdontologo.setAdapter(adapter)
                            binding.autoOdontologo.showDropDown()
                        }
                    }
                }
            }
        })
    }

    //Cargar el odontólogo actual
    private fun cargarOdontologoPorId(odontologoId: String) {
        val url = "$API_USUARIOS/$odontologoId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EditCitas,
                        "⚠️ Error al obtener odontólogo actual",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return

                    val json = JSONObject(response.body!!.string())
                    val data = json.optJSONObject("data") ?: return

                    val nombreCompleto =
                        "${data.optString("nombres", "")} ${data.optString("apellidos", "")}".trim()

                    runOnUiThread {
                        val odontologo = OdontologoItem(nombreCompleto, odontologoId)
                        val adapter = ArrayAdapter(
                            this@EditCitas,
                            android.R.layout.simple_dropdown_item_1line,
                            listOf(odontologo)
                        )
                        binding.autoOdontologo.setAdapter(adapter)
                        binding.autoOdontologo.setText(nombreCompleto, false)
                        odontologoSeleccionado = odontologo
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
        val pacienteId = pacienteSeleccionado?.id
        if (pacienteId == null) {
            Toast.makeText(this, "⚠️ Selecciona un paciente válido", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioId = odontologoSeleccionado?.id
        if (usuarioId == null) {
            Toast.makeText(this, "⚠️ Selecciona un odontólogo válido", Toast.LENGTH_SHORT).show()
            return
        }

        val motivo = binding.etMotivo.text.toString().trim()

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
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val citaId = intent.getStringExtra("CITA_ID") ?: return
        val urlFinal = "$API_URL/$citaId"

        val requestBuilder = Request.Builder()
            .url(urlFinal)
            .header("Content-Type", "application/json")

        val request = requestBuilder.patch(body).build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val respuesta = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val mensaje = "Cita actualizada correctamente ✅"

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
    }
    private fun cargarCitaPorId(citaId: String) {
        val request = Request.Builder()
            .url("$API_URL/$citaId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@EditCitas, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error de conexión")
                        .setContentText("No se pudo cargar la cita ❌")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            SweetAlertDialog(this@EditCitas, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Error")
                                .setContentText("Error al obtener la cita (${response.code})")
                                .show()
                        }
                        return
                    }

                    val json = JSONObject(it.body!!.string())
                    val data = json.getJSONObject("data")

                    val fecha = data.optString("fecha", "")
                    val motivo = data.optString("motivo", "")
                    val estado = data.optString("estado", "Pendiente")
                    val pacienteId = data.optString("paciente_id", "")
                    val pacienteNombre = data.optString("paciente_nombre", pacienteId)
                    val usuarioId = data.optString("usuario_id", "")

                    runOnUiThread {
                        // Mostrar los datos en los campos
                        binding.etFecha.setText(fecha.replace("T", " ").replace("Z", ""))
                        binding.etMotivo.setText(motivo)

                        runOnUiThread {
                            val pacientes = listOf(PacienteItem(pacienteNombre, pacienteId))
                            val pacAdapter = PacienteAdapter(this@EditCitas, pacientes)
                            binding.autoPaciente.setAdapter(pacAdapter)
                            bloqueandoBusqueda = true
                            binding.autoPaciente.setText(pacientes[0].toString(), false)
                            pacienteSeleccionado = pacientes[0]
                            val usuarioNombre = data.optString("usuario_nombre", usuarioId)

                            // Desactivar búsqueda tras breve pausa
                            binding.autoPaciente.postDelayed({
                                bloqueandoBusqueda = false
                            }, 500)
                            cargarOdontologoPorId(usuarioId)
                        }
                    }
                }
            }
        })
    }

}
