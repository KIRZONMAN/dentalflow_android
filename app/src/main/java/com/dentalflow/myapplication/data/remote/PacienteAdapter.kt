package com.dentalflow.myapplication.data.remote

import android.widget.ArrayAdapter
import com.dentalflow.myapplication.Asistente.EditCitas

data class PacienteItem(val nombre: String, val id: String) {
    override fun toString(): String = "$nombre ($id)"
}
class PacienteAdapter(context: android.content.Context, pacientes: List<PacienteItem>) :
    ArrayAdapter<PacienteItem>(
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
                clear() // ahora sí, seguro
                if (results?.values != null) {
                    @Suppress("UNCHECKED_CAST")
                    addAll(results.values as List<PacienteItem>)
                }
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any): CharSequence {
                return (resultValue as PacienteItem).toString()
            }
        }
    }

}

