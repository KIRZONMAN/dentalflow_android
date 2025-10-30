package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.databinding.ItemCitaBinding
import com.dentalflow.myapplication.data.remote.model.Cita

class CitaAdapter(
    private var citas: List<Cita>
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    inner class CitaViewHolder(val binding: ItemCitaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val binding = ItemCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        with(holder.binding) {
            txtFecha.text = cita.fecha.substring(0, 10) // formato YYYY-MM-DD
            txtHora.text = cita.fecha.substring(11, 16) // formato HH:MM
            txtEstado.text = cita.estado
            txtMotivo.text = cita.motivo ?: "-"
            txtTotal.text = cita.total?.toString() ?: "0"
            txtCedula.text = cita.paciente_id
            txtPacienteNombre.text = cita.paciente_nombre ?: "Desconocido"
            txtOdontologo.text = cita.usuario_id


            btnAceptar.setOnClickListener {
                Toast.makeText(root.context, "Cita ${cita._id} aceptada", Toast.LENGTH_SHORT).show()
            }
            btnCancelar.setOnClickListener {
                Toast.makeText(root.context, "Cita ${cita._id} cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = citas.size

    fun updateData(newList: List<Cita>) {
        citas = newList
        notifyDataSetChanged()
    }
}
