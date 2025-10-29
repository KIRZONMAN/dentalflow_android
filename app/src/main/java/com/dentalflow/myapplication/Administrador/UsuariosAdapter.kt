package com.dentalflow.myapplication.Administrador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.databinding.ItemUsuarioRowBinding

class UsuariosAdapter(
    private val onEdit: (UsuarioUi) -> Unit = {},
    private val onDelete: (UsuarioUi) -> Unit = {}
) : ListAdapter<UsuarioUi, UsuariosAdapter.VH>(Diff()) {

    class VH(val b: ItemUsuarioRowBinding) : RecyclerView.ViewHolder(b.root)

    // 6 columnas: ID, Nombre, Correo, Estado, Rol, Acciones
    private var colW = IntArray(6) { ViewGroup.LayoutParams.WRAP_CONTENT }

    fun updateColumnWidths(w: IntArray) {
        colW = if (w.size >= 6) w.copyOf(6)
        else (w + IntArray(6 - w.size) { ViewGroup.LayoutParams.WRAP_CONTENT })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemUsuarioRowBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)

        holder.b.tvId.text = user.id?.takeLast(6)
        holder.b.tvNombre.text = user.nombre
        holder.b.tvCorreo.text = user.correo
        holder.b.tvEstado.text = user.estado ?: ""
        holder.b.tvRol.text = user.rol ?: ""

        // Anchos por columna
        holder.b.tvId.layoutParams.width = colW[0]
        holder.b.tvNombre.layoutParams.width = colW[1]
        holder.b.tvCorreo.layoutParams.width = colW[2]
        holder.b.tvEstado.layoutParams.width = colW[3]
        holder.b.tvRol.layoutParams.width = colW[4]
        holder.b.cellAcciones.layoutParams.width = colW[5] // << ANCHO de la celda Acciones

        holder.b.tvId.requestLayout()
        holder.b.tvNombre.requestLayout()
        holder.b.tvCorreo.requestLayout()
        holder.b.tvEstado.requestLayout()
        holder.b.tvRol.requestLayout()
        holder.b.cellAcciones.requestLayout()

        // Callbacks correctos (evita el "Type mismatch: View! vs UsuarioUi")
        holder.b.btnEditar.setOnClickListener { onEdit(user) }
        holder.b.btnEliminar.setOnClickListener { onDelete(user) }
    }

    class Diff : DiffUtil.ItemCallback<UsuarioUi>() {
        override fun areItemsTheSame(old: UsuarioUi, new: UsuarioUi) = old.id == new.id
        override fun areContentsTheSame(old: UsuarioUi, new: UsuarioUi) = old == new
    }
}
