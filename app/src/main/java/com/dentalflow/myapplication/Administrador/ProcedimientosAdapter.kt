package com.dentalflow.myapplication.Administrador

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.R

class ProcedimientosAdapter(
    private val currencyFormatter: (Double) -> String,
    private val onDelete: (Procedimiento) -> Unit
) : ListAdapter<Procedimiento, ProcedimientosAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Procedimiento>() {
            override fun areItemsTheSame(old: Procedimiento, new: Procedimiento) = old.id == new.id
            override fun areContentsTheSame(old: Procedimiento, new: Procedimiento) = old == new
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvId: TextView = v.findViewById(R.id.tvId)
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvCosto: TextView = v.findViewById(R.id.tvCosto)
        val btnDel: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_procedimiento, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvId.text = item.id.toString()
        holder.tvNombre.text = item.nombre
        holder.tvCosto.text = currencyFormatter(item.costo)
        holder.btnDel.setOnClickListener { onDelete(item) }
    }
}
