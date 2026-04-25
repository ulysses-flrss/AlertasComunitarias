package com.example.alertascomunitarias.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R

class AlertAdapter(
    private val alertList: List<ProfileActivity.AlertItem>,
    private val currentUserId: String, // 🔥 Necesario para saber de quién es la alerta
    private val onAlertClick: (ProfileActivity.AlertItem) -> Unit,
    private val onResolveClick: (ProfileActivity.AlertItem) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvItemCategory)
        val tvDistance: TextView = itemView.findViewById(R.id.tvItemDistance) // 🔥 LA DISTANCIA
        val tvDate: TextView = itemView.findViewById(R.id.tvItemDate)
        val tvDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
        val tvStatus: TextView = itemView.findViewById(R.id.tvItemStatus)
        val btnResolve: Button = itemView.findViewById(R.id.btnResolveAlert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_user, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]

        // Textos básicos
        holder.tvCategory.text = alert.category
        holder.tvDescription.text = if (alert.description.isEmpty()) "Sin descripción" else alert.description
        holder.tvDate.text = alert.date

        // 🔥 MOSTRAR U OCULTAR LA DISTANCIA
        if (alert.distance.isNotEmpty()) {
            holder.tvDistance.visibility = View.VISIBLE
            holder.tvDistance.text = "📍 ${alert.distance}"
        } else {
            holder.tvDistance.visibility = View.GONE
        }

        // Estado capitalizado (ej: "Activo", "Resuelto")
        val estadoStr = alert.status.lowercase()
        holder.tvStatus.text = "Estado: ${alert.status.replaceFirstChar { it.uppercase() }}"

        // 🔥 REGLA MAESTRA: Solo verde y con botón SI está activa Y SI le pertenece al usuario actual
        if ((estadoStr == "active" || estadoStr == "activo") && alert.userId == currentUserId) {

            holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
            holder.btnResolve.visibility = View.VISIBLE

            holder.btnResolve.setOnClickListener {
                onResolveClick(alert)
            }

        } else {
            // Si está resuelta O si es de otra persona, ocultamos el botón
            if (estadoStr == "active" || estadoStr == "activo") {
                holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                holder.tvStatus.setTextColor(holder.itemView.context.resources.getColor(android.R.color.darker_gray, null))
            }
            holder.btnResolve.visibility = View.GONE
        }

        // Tocar la tarjeta completa para abrir el detalle
        holder.itemView.setOnClickListener {
            onAlertClick(alert)
        }
    }

    override fun getItemCount(): Int {
        return alertList.size
    }
}