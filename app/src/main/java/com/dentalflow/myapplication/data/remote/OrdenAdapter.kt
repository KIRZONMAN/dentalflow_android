package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.OrdenCompra

class OrdenAdapter(private val lista: List<OrdenCompra>) :
    RecyclerView.Adapter<OrdenAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvId = v.findViewById<TextView>(R.id.tvId)
        val tvEstado = v.findViewById<TextView>(R.id.tvEstado)
        val tvTotal = v.findViewById<TextView>(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_estado, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val o = lista[pos]
        holder.tvId.text = o._id
        holder.tvEstado.text = o.estado
        holder.tvTotal.text = "Total: ${o.total}"
    }

    override fun getItemCount() = lista.size
}
