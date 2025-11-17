package com.dentalflow.myapplication.Odontologo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dentalflow.myapplication.data.remote.HistoriaAdapter
import com.dentalflow.myapplication.data.remote.model.Alergia
import com.dentalflow.myapplication.data.remote.model.Antecedente
import com.dentalflow.myapplication.data.remote.model.HistoriaClinicaDto
import com.dentalflow.myapplication.data.remote.model.HistoriaItem
import com.dentalflow.myapplication.data.remote.model.Procedimiento
import com.dentalflow.myapplication.data.remote.model.Receta
import com.dentalflow.myapplication.databinding.ActivityVerHistoriaBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ActivityVerHistoria : AppCompatActivity() {

    private lateinit var binding: ActivityVerHistoriaBinding
    private val client = OkHttpClient()
    private val BASE_URL = "https://lucid-youthfulness-production.up.railway.app/api/historias-clinicas/paciente/"
    private lateinit var adapter: HistoriaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerHistoriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pacienteId = intent.getStringExtra("PACIENTE_ID") ?: return
        val nombrePaciente = intent.getStringExtra("PACIENTE_NOMBRE") ?: ""

        binding.tvNombreUsuario.text = nombrePaciente

        adapter = HistoriaAdapter(mutableListOf())
        binding.rvHistoria.layoutManager = LinearLayoutManager(this)
        binding.rvHistoria.adapter = adapter

        obtenerHistoria(pacienteId)

        binding.btnVolver.setOnClickListener { finish() }
    }

    private fun obtenerHistoria(id: String) {
        val request = Request.Builder()
            .url(BASE_URL + id)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    SweetAlertDialog(this@ActivityVerHistoria, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("⚠️ Error de conexión")
                        .setContentText(e.message ?: "No se pudo contactar el servidor")
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return

                try {
                    val json = JSONObject(body).getJSONObject("data")

                    // ---------------------
                    // PARSEO DE ANTECEDENTES
                    // ---------------------
                    val antecedentesArray = json.getJSONArray("antecedentes_medicos")
                    val listaAntecedentes = mutableListOf<Antecedente>()
                    for (i in 0 until antecedentesArray.length()) {
                        val obj = antecedentesArray.getJSONObject(i)
                        listaAntecedentes.add(
                            Antecedente(
                                enfermedad = obj.optString("enfermedad"),
                                gravedad = obj.optString("gravedad")
                            )
                        )
                    }

                    // ---------------------
                    // PARSEO DE ALERGIAS
                    // ---------------------
                    val alergiasArray = json.getJSONArray("alergias")
                    val listaAlergias = mutableListOf<Alergia>()
                    for (i in 0 until alergiasArray.length()) {
                        val obj = alergiasArray.getJSONObject(i)
                        listaAlergias.add(
                            Alergia(
                                alergia_a = obj.optString("alergia_a"),
                                reaccion = obj.optString("reaccion")
                            )
                        )
                    }

                    // ---------------------
                    // PARSEO DE RECETAS
                    // ---------------------
                    val recetasArray = json.getJSONArray("recetas_medicas")
                    val listaRecetas = mutableListOf<Receta>()
                    for (i in 0 until recetasArray.length()) {
                        val obj = recetasArray.getJSONObject(i)
                        listaRecetas.add(
                            Receta(
                                tipo_orden = obj.optString("tipo_orden"),
                                medicamento = obj.optString("medicamento"),
                                descripcion = obj.optString("descripcion"),
                                fecha_receta = obj.optString("fecha_receta")
                            )
                        )
                    }

                    // ---------------------
                    // PARSEO DE PROCEDIMIENTOS
                    // ---------------------
                    val procArray = json.getJSONArray("procedimientos_realizados")
                    val listaProcedimientos = mutableListOf<Procedimiento>()
                    for (i in 0 until procArray.length()) {
                        val obj = procArray.getJSONObject(i)
                        listaProcedimientos.add(
                            Procedimiento(
                                tratamiento = obj.optString("tratamiento"),
                                fecha = obj.optString("fecha"),
                                odontologo = obj.optString("odontologo"),
                                resultado = obj.optString("resultado")
                            )
                        )
                    }

                    // =====================================================
                    // CONVERTIR TODO EN ITEMS PARA EL RECYCLERVIEW
                    // =====================================================

                    // 1) IZQUIERDA: antecedentes + alergias + recetas
                    val listIzq = mutableListOf<String>()

                    listaAntecedentes.forEach {
                        listIzq.add("${it.enfermedad} (${it.gravedad})")
                    }

                    listaAlergias.forEach {
                        listIzq.add("${it.alergia_a} → ${it.reaccion}")
                    }

                    listaRecetas.forEach {
                        listIzq.add("${it.tipo_orden}: ${it.medicamento}\n${it.descripcion}")
                    }

                    // 2) DERECHA: procedimientos formateados
                    val listDer = listaProcedimientos.map {
                        "• ${it.tratamiento}\n${it.odontologo}\n${it.fecha.substring(0,10)}\n${it.resultado}"
                    }

                    // 3) EMPAREJAR AMBAS LISTAS
                    val maxSize = maxOf(listIzq.size, listDer.size)
                    val finalList = mutableListOf<HistoriaItem>()

                    for (i in 0 until maxSize) {
                        finalList.add(
                            HistoriaItem(
                                columnaIzq = listIzq.getOrNull(i) ?: "",
                                procedimiento = listDer.getOrNull(i)
                            )
                        )
                    }

                    // 4) Cargar al adaptador
                    runOnUiThread {
                        adapter.actualizar(finalList)
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        SweetAlertDialog(this@ActivityVerHistoria, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("⚠️ Error procesando datos")
                            .setContentText(e.message ?: "Error inesperado")
                            .show()
                    }
                }
            }

        })
    }
}
