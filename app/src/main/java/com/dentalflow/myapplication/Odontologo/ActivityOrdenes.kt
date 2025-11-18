package com.dentalflow.myapplication.Odontologo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.math.max

class ActivityOrdenes : AppCompatActivity() {

    private lateinit var btnGuardar: Button
    private lateinit var btnVolver: Button
    private lateinit var spPacienteReceta: Spinner
    private lateinit var etMedicamento: EditText
    private lateinit var etTipoOrden: EditText
    private lateinit var etDescripcionOrden: EditText

    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api/historias-clinicas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ordenes)

        btnGuardar = findViewById(R.id.btnGuardar)
        btnVolver = findViewById(R.id.btnVolver)
        spPacienteReceta = findViewById(R.id.spPacienteReceta)
        etMedicamento = findViewById(R.id.etMedicamento)
        etTipoOrden = findViewById(R.id.etTipoOrden)
        etDescripcionOrden = findViewById(R.id.etDescripcionOrden)

        btnGuardar.setOnClickListener {
            guardarOrden()
        }
        cargarPacientes()
        btnVolver.setOnClickListener { finish() }
    }

    // OBTENER HISTORIA POR PACIENTE
    private fun obtenerHistoriaPorPaciente(pacienteId: String, callback: (HistoriaClinicaDto?) -> Unit) {

        val url = "https://lucid-youthfulness-production.up.railway.app/api/historias-clinicas?paciente_id=$pacienteId"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityOrdenes, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error de conexión")
                        .setContentText(e.message ?: "No se pudo contactar el servidor")
                        .show()
                }
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return callback(null)

                try {
                    val json = JSONObject(body)

                    if (!json.getBoolean("ok")) {
                        callback(null)
                        return
                    }

                    val dataArray = json.getJSONArray("data")

                    // Si el paciente NO tiene historias clínicas
                    if (dataArray.length() == 0) {
                        callback(null)
                        return
                    }

                    // Tomar la primera historia
                    val historiaObj = dataArray.getJSONObject(0)
                    callback(parseHistoria(historiaObj))

                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
    private fun cargarPacientes() {
        val url = "https://lucid-youthfulness-production.up.railway.app/api/pacientes"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityOrdenes, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error cargando pacientes")
                        .setContentText(e.message ?: "No se pudo contactar el servidor")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return

                try {
                    val json = JSONObject(body)
                    val data = json.getJSONArray("data")

                    val listaPacientes = mutableListOf<String>()

                    for (i in 0 until data.length()) {
                        val p = data.getJSONObject(i)
                        listaPacientes.add(p.getString("_id"))   // ID REAL DEL PACIENTE
                    }

                    runOnUiThread {
                        val adapter = android.widget.ArrayAdapter(
                            this@ActivityOrdenes,
                            android.R.layout.simple_spinner_item,
                            listaPacientes
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spPacienteReceta.adapter = adapter
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityOrdenes, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error parseando pacientes")
                            .setContentText(e.message ?: "JSON inválido")
                            .show()
                    }
                }
            }
        })
    }
    // ============================================================
    // PARSEAR HISTORIA CLÍNICA
    // ============================================================
    private fun parseHistoria(o: JSONObject): HistoriaClinicaDto {
        return HistoriaClinicaDto(
            _id = o.getString("_id"),
            paciente_id = o.getString("paciente_id"),
            antecedentes_medicos = parseArray(o.optJSONArray("antecedentes_medicos")) { item ->
                Antecedente(
                    enfermedad = item.optString("enfermedad"),
                    gravedad = item.optString("gravedad")
                )
            },
            alergias = parseArray(o.optJSONArray("alergias")) { item ->
                Alergia(
                    alergia_a = item.optString("alergia_a"),
                    reaccion = item.optString("reaccion")
                )
            },
            recetas_medicas = parseArray(o.optJSONArray("recetas_medicas")) { item ->
                Receta(
                    tipo_orden = item.optString("tipo_orden"),
                    medicamento = item.optString("medicamento"),
                    descripcion = item.optString("descripcion"),
                    fecha_receta = extractDate(item)
                )
            },
            procedimientos_realizados = parseArray(o.optJSONArray("procedimientos_realizados")) { item ->
                Procedimiento(
                    tratamiento = item.optString("tratamiento"),
                    fecha = extractDate(item),
                    odontologo = item.optString("odontologo"),
                    resultado = item.optString("resultado")
                )
            }
        )
    }

    private fun <T> parseArray(arr: JSONArray?, mapFn: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        val list = mutableListOf<T>()
        for (i in 0 until arr.length()) {
            list.add(mapFn(arr.getJSONObject(i)))
        }
        return list
    }

    // Admite JSON (string) y BSON ($date)
    private fun extractDate(obj: JSONObject): String? {
        return when {
            obj.has("fecha") && obj.get("fecha") is String -> obj.getString("fecha")
            obj.has("fecha") && obj.get("fecha") is JSONObject -> obj.getJSONObject("fecha").optString("\$date")

            obj.has("fecha_receta") && obj.get("fecha_receta") is String -> obj.getString("fecha_receta")
            obj.has("fecha_receta") && obj.get("fecha_receta") is JSONObject ->
                obj.getJSONObject("fecha_receta").optString("\$date")

            else -> null
        }
    }

    // ============================================================
    // CREAR NUEVA ORDEN/RECETA
    // ============================================================
    private fun construirNuevaReceta(tipoOrden: String, medicamento: String?, descripcion: String?): Receta {
        val fechaActual = java.time.Instant.now().toString()

        return Receta(
            tipo_orden = tipoOrden,
            medicamento = medicamento,
            descripcion = descripcion,
            fecha_receta = fechaActual
        )
    }

    // ============================================================
    // PATCH → AGREGAR A LA HISTORIA
    // ============================================================
    private fun agregarRecetaAHistoria(
        historia: HistoriaClinicaDto,
        nuevaReceta: Receta,
        callback: (Boolean) -> Unit
    ) {
        val url = "$BASE_URL/${historia._id}"

        val recetasActualizadas = historia.recetas_medicas.toMutableList()
        recetasActualizadas.add(nuevaReceta)

        val bodyJson = JSONObject().apply {
            put("recetas_medicas", JSONArray().apply {
                recetasActualizadas.forEach { r ->
                    put(JSONObject().apply {
                        put("tipo_orden", r.tipo_orden)
                        put("medicamento", r.medicamento)
                        put("descripcion", r.descripcion)
                        put("fecha_receta", r.fecha_receta)
                    })
                }
            })
        }

        val body = bodyJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .patch(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(false)
            override fun onResponse(call: Call, response: Response) =
                callback(response.isSuccessful)
        })
    }

    private fun guardarOrden() {
        val pacienteId = spPacienteReceta.selectedItem?.toString()

        if (pacienteId == null) {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Selecciona un paciente")
                .show()
            return
        }
        val medicamento = etMedicamento.text.toString()
        val tipoOrden = etTipoOrden.text.toString()
        val descripcion = etDescripcionOrden.text.toString()

        if (tipoOrden.isEmpty() || descripcion.isEmpty()) {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Campos incompletos")
                .setContentText("Debes ingresar tipo de orden y descripción.")
                .show()
            return
        }

        obtenerHistoriaPorPaciente(pacienteId) { historia ->
            if (historia == null) {
                runOnUiThread {
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Historia no encontrada")
                        .show()
                }
                return@obtenerHistoriaPorPaciente
            }

            val nuevaReceta = construirNuevaReceta(tipoOrden, medicamento, descripcion)

            agregarRecetaAHistoria(historia, nuevaReceta) { ok ->
                runOnUiThread {
                    if (ok)
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Guardado")
                            .setContentText("La orden/receta fue agregada correctamente.")
                            .show()
                    else
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("No se pudo actualizar la historia clínica.")
                            .show()
                }
            }
        }
    }
}
