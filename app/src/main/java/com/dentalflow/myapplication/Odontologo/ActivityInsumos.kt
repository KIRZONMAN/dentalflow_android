package com.dentalflow.myapplication.Odontologo

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.InsumoAdapter
import com.dentalflow.myapplication.data.remote.OrdenAdapter
import com.dentalflow.myapplication.data.remote.model.Insumo
import com.dentalflow.myapplication.data.remote.model.OrdenCompra
import com.dentalflow.myapplication.data.remote.model.Proveedor
import com.dentalflow.myapplication.databinding.ActivityInsumosBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val BASE_URL = "http://10.0.2.2:3000/api"
private const val API_INSUMOS = "$BASE_URL/insumos" //NO EXISTE
private const val API_PROVEEDORES = "$BASE_URL/proveedores" // NO EXISTE
private const val API_ORDENES = "$BASE_URL/ordenes-compras"
class ActivityInsumos : AppCompatActivity() {

    private lateinit var binding: ActivityInsumosBinding
    private val client = OkHttpClient()

    private var listaInsumos = mutableListOf<Insumo>()
    private var listaProveedores = mutableListOf<Proveedor>()
    private var listaEstados = mutableListOf<OrdenCompra>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsumosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        cargarInsumos()
        cargarProveedores()
        cargarEstados()

        binding.btnSolicitar.setOnClickListener { enviarSolicitud() }
        binding.btnLimpiar.setOnClickListener { limpiarCampos() }
    }

    private fun setupRecyclerViews() {
        binding.rvInsumosDisponibles.layoutManager = LinearLayoutManager(this)
        binding.rvEstadosSolicitudes.layoutManager = LinearLayoutManager(this)
    }

    // ------------------ GET INSUMOS --------------------
    private fun cargarInsumos() {
        val req = Request.Builder().url(API_INSUMOS).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = showError("Error cargando insumos: ${e.message}")

            override fun onResponse(call: Call, res: Response) {
                val json = JSONObject(res.body!!.string())
                val data = json.getJSONArray("data")

                listaInsumos.clear()
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    listaInsumos.add(
                        Insumo(
                            o.getString("_id"),
                            o.getString("nombre"),
                            o.optDouble("costo_insumo", 0.0)
                        )
                    )
                }

                runOnUiThread {
                    binding.rvInsumosDisponibles.adapter = InsumoAdapter(listaInsumos)
                    binding.spTipoInsumo.adapter =
                        ArrayAdapter(this@ActivityInsumos, android.R.layout.simple_spinner_item,
                            listaInsumos.map { it.nombre })
                }
            }
        })
    }

    // ------------------ GET PROVEEDORES --------------------
    private fun cargarProveedores() {
        val req = Request.Builder().url(API_PROVEEDORES).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = showError("Error cargando proveedores: ${e.message}")

            override fun onResponse(call: Call, res: Response) {
                val json = JSONObject(res.body!!.string())
                val data = json.getJSONArray("data")

                listaProveedores.clear()
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    listaProveedores.add(
                        Proveedor(
                            o.getString("_id"),
                            o.getString("nombre")
                        )
                    )
                }

                runOnUiThread {
                    binding.spProveedor.adapter =
                        ArrayAdapter(this@ActivityInsumos, android.R.layout.simple_spinner_item,
                            listaProveedores.map { it.nombre })
                }
            }
        })
    }

    // ------------------ GET ORDENES (ESTADOS) --------------------
    private fun cargarEstados() {
        val req = Request.Builder().url(API_ORDENES).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = showError("Error cargando estados: ${e.message}")

            override fun onResponse(call: Call, res: Response) {
                val json = JSONObject(res.body!!.string())
                val data = json.getJSONArray("data")

                listaEstados.clear()
                for (i in 0 until data.length()) {
                    val o = data.getJSONObject(i)
                    listaEstados.add(
                        OrdenCompra(
                            o.getString("_id"),
                            o.getString("proveedor_id"),
                            o.getString("usuario_id"),
                            o.getString("estado"),
                            o.getString("fecha_expedicion"),
                            o.getDouble("total")
                        )
                    )
                }

                runOnUiThread {
                    binding.rvEstadosSolicitudes.adapter = OrdenAdapter(listaEstados)
                }
            }
        })
    }

    // ------------------ POST CREAR SOLICITUD --------------------
    private fun enviarSolicitud() {
        val cantidadStr = binding.etCantidad.text.toString().trim()
        if (cantidadStr.isEmpty()) return showError("Ingrese cantidad")

        val insumo = listaInsumos[binding.spTipoInsumo.selectedItemPosition]
        val proveedor = listaProveedores[binding.spProveedor.selectedItemPosition]
        val cantidad = cantidadStr.toInt()

        val json = JSONObject().apply {
            put("proveedor_id", proveedor._id)
            put("usuario_id", "68cf0ae4a25f917fc87112ae") // ← reemplazar con el real
            put("estado", "Enviada")
            put("detalles", JSONArray().put(
                JSONObject().apply {
                    put("insumo_id", insumo._id)
                    put("cantidad", cantidad)
                    put("precio_unit", insumo.precio_unit)
                }
            ))
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(API_ORDENES).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = showError("Error enviando solicitud: ${e.message}")

            override fun onResponse(call: Call, res: Response) {
                val txt = res.body!!.string()
                val ok = JSONObject(txt).optBoolean("ok")

                runOnUiThread {
                    SweetAlertDialog(this@ActivityInsumos, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Enviado")
                        .setContentText("Solicitud registrada correctamente")
                        .show()

                    limpiarCampos()
                    cargarEstados()
                }
            }
        })
    }

    private fun limpiarCampos() {
        binding.etCantidad.setText("")
        binding.spTipoInsumo.setSelection(0)
        binding.spProveedor.setSelection(0)
    }

    private fun showError(msg: String) {
        runOnUiThread {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error")
                .setContentText(msg)
                .show()
        }
    }
}
