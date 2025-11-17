package com.dentalflow.myapplication.Odontologo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.Cita
import com.dentalflow.myapplication.data.remote.model.OrdenLabCreate
import com.dentalflow.myapplication.data.remote.model.ProductoLab
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import cn.pedant.SweetAlert.SweetAlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SolicitudProtesis : AppCompatActivity() {

    private lateinit var spCita: Spinner
    private lateinit var etTipoPedido: EditText
    private lateinit var cbMetalPorcelana: CheckBox
    private lateinit var cbZirconio: CheckBox
    private lateinit var cbProtesisTotal: CheckBox
    private lateinit var cbProtesisParcial: CheckBox
    private lateinit var cbOrtodoncia: CheckBox
    private lateinit var cbSuperior: CheckBox
    private lateinit var cbInferior: CheckBox
    private lateinit var cbAcrilico: CheckBox
    private lateinit var cbFlexible: CheckBox
    private lateinit var etColor: EditText
    private lateinit var btnSubirFirma: Button
    private lateinit var btnEnviar: MaterialButton

    private var uriFirma: Uri? = null
    private val PICK_IMAGE = 101

    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api"
    private val API_ORDENESLAB = "$BASE_URL/ordenes-laboratorio"
    private val API_CITAS = "$BASE_URL/citas"

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitud_protesis)

        initViews()
        obtenerCitas() // 🔥 Cargar citas al iniciar

        btnSubirFirma.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnEnviar.setOnClickListener {
            enviarSolicitud()
        }
    }

    private fun initViews() {
        spCita = findViewById(R.id.spCita)
        etTipoPedido = findViewById(R.id.etTipoPedido)
        etColor = findViewById(R.id.etColor)

        cbMetalPorcelana = findViewById(R.id.cbMetalPorcelana)
        cbZirconio = findViewById(R.id.cbZirconio)
        cbProtesisTotal = findViewById(R.id.cbProtesisTotal)
        cbProtesisParcial = findViewById(R.id.cbProtesisParcial)
        cbOrtodoncia = findViewById(R.id.cbOrtodoncia)
        cbSuperior = findViewById(R.id.cbSuperior)
        cbInferior = findViewById(R.id.cbInferior)
        cbAcrilico = findViewById(R.id.cbAcrilico)
        cbFlexible = findViewById(R.id.cbFlexible)

        btnSubirFirma = findViewById(R.id.btnSubirFirma)
        btnEnviar = findViewById(R.id.btnEnviar)
    }

    // ---------------------------
    // Selección de imagen (firma)
    // ---------------------------
    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uriFirma = uri
            Toast.makeText(this, "Firma seleccionada correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            uriFirma = data?.data
            Toast.makeText(this, "Firma seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------
    // 🔥 OBTENER CITAS DESDE LA API Y LLENAR EL SPINNER
    // ------------------------------------------------------
    private fun obtenerCitas() {
        val request = Request.Builder()
            .url(API_CITAS)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("⚠️ Error de conexión")
                        .setContentText(e.message ?: "No se pudo conectar al servidor")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return

                if (!response.isSuccessful) {
                    runOnUiThread {
                        SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("❌ Error")
                            .setContentText("Código: ${response.code}")
                            .show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val dataArray = json.getJSONArray("data")
                    val lista = mutableListOf<Cita>()

                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)

                        val cita = Cita(
                            _id = obj.getString("_id"),
                            fecha = obj.getString("fecha"),
                            paciente_nombre = obj.optString("paciente_nombre", "Desconocido"),
                            estado = obj.optString("estado", "Pendiente"),
                            motivo = obj.optString("motivo", ""),
                            total = obj.optDouble("total", 0.0),
                            paciente_id = obj.optString("paciente_id", ""),
                            usuario_id = obj.optString("usuario_id", "")
                        )
                        lista.add(cita)
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@SolicitudProtesis,
                            android.R.layout.simple_spinner_item,
                            lista
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spCita.adapter = adapter
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("⚠️ Error parseando datos")
                            .setContentText(e.message)
                            .show()
                    }
                }
            }
        })
    }

    // ------------------------------------------------------
    // Enviar solicitud
    // ------------------------------------------------------
    private fun enviarSolicitud() {

        val citaSeleccionada = spCita.selectedItem as? Cita
        if (citaSeleccionada == null) {
            Toast.makeText(this, "Seleccione una cita", Toast.LENGTH_LONG).show()
            return
        }

        val citaId = citaSeleccionada._id
        val usuarioId = "68cf25706f565e081eca7c7d" // cambiar por usuario real

        val tipoPedido = etTipoPedido.text.toString().trim()
        val color = etColor.text.toString().trim()

        if (tipoPedido.isEmpty()) {
            etTipoPedido.error = "Requerido"
            return
        }

        // ------------------------------
        // ARMAR PRODUCTOS
        // ------------------------------
        val productos = mutableListOf<ProductoLab>()

        if (cbMetalPorcelana.isChecked)
            productos.add(ProductoLab("Metal Porcelana", tipoPedido, 1))

        if (cbZirconio.isChecked)
            productos.add(ProductoLab("Zirconio", tipoPedido, 1))

        if (cbProtesisTotal.isChecked)
            productos.add(ProductoLab("Prótesis Total", tipoPedido, 1))

        if (cbProtesisParcial.isChecked)
            productos.add(ProductoLab("Prótesis Parcial", tipoPedido, 1))

        if (cbOrtodoncia.isChecked)
            productos.add(ProductoLab("Ortodoncia", tipoPedido, 1))

        if (cbSuperior.isChecked)
            productos.add(ProductoLab("Superior", tipoPedido, 1))

        if (cbInferior.isChecked)
            productos.add(ProductoLab("Inferior", tipoPedido, 1))

        if (cbAcrilico.isChecked)
            productos.add(ProductoLab("Acrílico", tipoPedido, 1))

        if (cbFlexible.isChecked)
            productos.add(ProductoLab("Flexible", tipoPedido, 1))

        if (productos.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un producto", Toast.LENGTH_LONG).show()
            return
        }

        // -----------------------------------
        // ✔ OBSERVACIONES -> COMO OBJETO
        // -----------------------------------
        val observacionesObj = "Color: $color"

        val bodyObj = OrdenLabCreate(
            cita_id = citaId,
            usuario_id = usuarioId,
            fecha_creacion = null,
            estado = "Pendiente",
            observaciones = observacionesObj,
            productos = productos
        )

        val json = Gson().toJson(bodyObj)
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_ORDENESLAB)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error al enviar")
                        .setContentText(e.message)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (!response.isSuccessful) {
                        SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("❌ Error al insertar")
                            .setContentText("Código: ${response.code}\n${body}")
                            .show()
                        return@runOnUiThread
                    }

                    SweetAlertDialog(this@SolicitudProtesis, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("✔️ Enviado")
                        .setContentText("Solicitud enviada con éxito")
                        .show()
                }
            }

        })
    }

}
