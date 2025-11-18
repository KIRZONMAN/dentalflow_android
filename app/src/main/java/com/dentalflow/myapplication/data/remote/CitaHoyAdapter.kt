package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.databinding.ItemCitaBinding
import org.json.JSONObject

class CitaHoyAdapter(
    private val lista: List<JSONObject>
) : RecyclerView.Adapter<CitaHoyAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCitaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCitaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.binding.txtFecha.text = item.optString("fecha")
        holder.binding.txtHora.text = item.optString("hora")
        holder.binding.txtEstado.text = item.optString("estado")
        holder.binding.txtMotivo.text = item.optString("motivo")
        holder.binding.txtTotal.text = item.optString("total")
        holder.binding.txtCedula.text = item.optString("cedula")
        holder.binding.txtPacienteNombre.text = item.optString("paciente")
        holder.binding.txtOdontologo.text = item.optString("odontologo")
    }

    override fun getItemCount(): Int = lista.size
}
