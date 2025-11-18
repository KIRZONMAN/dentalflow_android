package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.data.remote.model.Cita
import com.dentalflow.myapplication.databinding.ItemCitaBinding

class CitaAdapter(
    private var citas: List<Cita>,
    private val onEditClick: (Cita) -> Unit,
    private val onAceptarClick: (Cita) -> Unit,
    private val onCancelarClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    inner class CitaViewHolder(val binding: ItemCitaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val binding = ItemCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        with(holder.binding) {

            txtFecha.text = cita.fecha.substring(0, 10)
            txtHora.text = cita.fecha.substring(11, 16)
            txtEstado.text = cita.estado
            txtMotivo.text = cita.motivo ?: "-"
            txtTotal.text = cita.total?.toString() ?: "0"
            txtCedula.text = cita.paciente_id
            txtPacienteNombre.text = cita.paciente_nombre ?: "Desconocido"
            txtOdontologo.text = cita.usuario_nombre ?: "Cargando..."

            btnAceptar.setOnClickListener {
                onAceptarClick(cita)
            }

            btnCancelar.setOnClickListener {
                onCancelarClick(cita)
            }

            btnEditar.setOnClickListener {
                onEditClick(cita)
            }
        }
    }

    override fun getItemCount() = citas.size

    fun updateData(newList: List<Cita>) {
        citas = newList
        notifyDataSetChanged()
    }
}
