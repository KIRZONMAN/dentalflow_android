package com.dentalflow.myapplication.data.remote

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.PacienteDto

data class PacienteItem(val nombre: String, val id: String) {
    override fun toString(): String = nombre // Muestra solo el nombre en el campo
}
class PacientesAdapter(
    private var lista: List<PacienteDto>,
    private val onVerHistoria: (PacienteDto) -> Unit
) : RecyclerView.Adapter<PacientesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCedula: TextView = view.findViewById(R.id.tvCedula)
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvTelefono: TextView = view.findViewById(R.id.tvTelefono)
        val btnHistoria: Button = view.findViewById(R.id.btnHistoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paciente = lista[position]
        holder.tvCedula.text = paciente._id
        holder.tvNombre.text = paciente.nombreCompleto
        holder.tvTelefono.text = paciente.telefono ?: "N/A"

        holder.btnHistoria.setOnClickListener {
            onVerHistoria(paciente)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<PacienteDto>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}

class PacienteAdapter(
    context: Context,
    pacientes: List<PacienteItem>
) : ArrayAdapter<PacienteItem>(
    context,
    android.R.layout.simple_dropdown_item_1line,
    pacientes.toMutableList()
) {

    private val allPacientes = ArrayList(pacientes)

    override fun getFilter(): android.widget.Filter {
        return object : android.widget.Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrBlank()) {
                    results.values = allPacientes
                    results.count = allPacientes.size
                } else {
                    val search = constraint.toString().lowercase()
                    val filtrados = allPacientes.filter {
                        it.nombre.lowercase().contains(search) || it.id.contains(search)
                    }
                    results.values = filtrados
                    results.count = filtrados.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                if (results?.values != null) {
                    @Suppress("UNCHECKED_CAST")
                    addAll(results.values as List<PacienteItem>)
                }
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any): CharSequence {
                return (resultValue as PacienteItem).nombre
            }
        }
    }
}
