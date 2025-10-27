package com.dentalflow.myapplication.Administrador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.databinding.ItemProveedorBinding

class ProveedoresAdapter(
    private val onDelete: (Proveedor) -> Unit
) : ListAdapter<Proveedor, ProveedoresAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Proveedor>() {
            override fun areItemsTheSame(o: Proveedor, n: Proveedor) = o.nit == n.nit
            override fun areContentsTheSame(o: Proveedor, n: Proveedor) = o == n
        }
    }

    inner class VH(val b: ItemProveedorBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProveedorBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.txtNit.text = item.nit
        holder.b.txtNombre.text = item.nombre
        holder.b.txtTelefono.text = item.telefono
        holder.b.txtCorreo.text = item.correo
        holder.b.btnEliminar.setOnClickListener { onDelete(item) }
    }
}
