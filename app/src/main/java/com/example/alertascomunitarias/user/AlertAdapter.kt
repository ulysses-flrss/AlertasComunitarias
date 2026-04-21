package com.example.alertascomunitarias.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R

// El adaptador recibe la lista de alertas y una función para manejar los clics
class AlertAdapter(
    private val alertList: List<ProfileActivity.AlertItem>,
    private val onAlertClick: (ProfileActivity.AlertItem) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    // El ViewHolder es la clase que "encuentra" y guarda los elementos de tu diseño XML
    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvItemCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
        val tvStatus: TextView = itemView.findViewById(R.id.tvItemStatus)
    }

    // 1. Inflar (Crear) la vista para cada elemento de la lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_user, parent, false)
        return AlertViewHolder(view)
    }

    // 2. Conectar los datos de la alerta con los TextViews
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]

        holder.tvCategory.text = alert.category
        holder.tvDescription.text = if (alert.description.isEmpty()) "Sin descripción" else alert.description

        // Darle un poco de color al estado
        holder.tvStatus.text = "Estado: ${alert.status.capitalize()}"
        if (alert.status.lowercase() == "active" || alert.status.lowercase() == "activo") {
            holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(android.R.color.darker_gray, null))
        }

        // Detectar si el usuario toca la tarjeta completa
        holder.itemView.setOnClickListener {
            onAlertClick(alert)
        }
    }

    // 3. Decirle al RecyclerView cuántos elementos hay en total
    override fun getItemCount(): Int {
        return alertList.size
    }
}