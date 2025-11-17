package com.dentalflow.myapplication.data.remote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalflow.myapplication.R
import com.dentalflow.myapplication.data.remote.model.HistoriaClinicaDto
import com.dentalflow.myapplication.data.remote.model.HistoriaItem
import java.text.SimpleDateFormat
import java.util.Locale

class HistoriaAdapter(
    private var lista: MutableList<HistoriaItem>
) : RecyclerView.Adapter<HistoriaAdapter.Holder>() {

    inner class Holder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvResumen1 = view.findViewById<TextView>(R.id.tvResumen1)
        val tvResumen2 = view.findViewById<TextView>(R.id.tvResumen2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historia, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = lista[position]

        holder.tvResumen1.text = item.columnaIzq
        holder.tvResumen2.text = item.procedimiento ?: "" // si no hay, se queda vacío
    }

    override fun getItemCount(): Int = lista.size

    fun actualizar(nueva: List<HistoriaItem>) {
        lista.clear()
        lista.addAll(nueva)
        notifyDataSetChanged()
    }
}
