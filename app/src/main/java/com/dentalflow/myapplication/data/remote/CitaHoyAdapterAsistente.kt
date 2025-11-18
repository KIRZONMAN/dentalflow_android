package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.databinding.ItemCitaAsistenteBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CitaHoyAdapterAsistente(
    private val lista: List<JSONObject>
) : RecyclerView.Adapter<CitaHoyAdapterAsistente.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCitaAsistenteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCitaAsistenteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        // FORMATEAR FECHA
        val fechaIso = item.optString("fecha")
        val (fecha, hora) = formatearFecha(fechaIso)

        holder.binding.txtFecha.text = fecha
        holder.binding.txtHora.text = hora
        holder.binding.txtEstado.text = item.optString("estado")
        holder.binding.txtMotivo.text = item.optString("motivo")
        holder.binding.txtTotal.text = item.optString("total")
        holder.binding.txtCedula.text = item.optString("paciente_id")
        holder.binding.txtPacienteNombre.text = item.optString("paciente_nombre")
        holder.binding.txtOdontologo.text = item.optString("usuario_id")

    }

    private fun formatearFecha(fechaIso: String?): Pair<String, String> {
        if (fechaIso.isNullOrBlank()) return "Sin fecha" to "Sin hora"

        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val fecha = isoFormat.parse(fechaIso)

            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

            formatoFecha.format(fecha) to formatoHora.format(fecha)

        } catch (e: Exception) {
            "Fecha inválida" to "Hora inválida"
        }
    }

    override fun getItemCount(): Int = lista.size
}
