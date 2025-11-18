package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.Insumo

class InsumoAdapter(private val lista: List<Insumo>) :
    RecyclerView.Adapter<InsumoAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre = v.findViewById<TextView>(R.id.tvNombre)
        val tvPrecio = v.findViewById<TextView>(R.id.tvPrecio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_insumo, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val item = lista[pos]
        holder.tvNombre.text = item.nombre
        holder.tvPrecio.text = "${item.precio_unit}"
    }

    override fun getItemCount() = lista.size
}
