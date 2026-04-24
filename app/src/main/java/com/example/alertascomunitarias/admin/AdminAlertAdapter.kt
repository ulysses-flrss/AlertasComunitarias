package com.example.alertascomunitarias.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminAlertAdapter(
    private val alertList: List<AdminAlertItem>
) : RecyclerView.Adapter<AdminAlertAdapter.AdminViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvAdminCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvAdminDescription)
        val tvUser: TextView = itemView.findViewById(R.id.tvAdminUser)
        val btnResolve: Button = itemView.findViewById(R.id.btnResolve)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_admin_alert_item, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {

        val alert = alertList[position]

        holder.tvCategory.text = alert.category
        holder.tvDescription.text = alert.description
        holder.tvUser.text = "Usuario: ${alert.userName}"

        // Marcar como resuelta
        holder.btnResolve.setOnClickListener {
            db.collection("alerts").document(alert.id)
                .update("status", "resolved")
        }

        // Eliminar alerta
        holder.btnDelete.setOnClickListener {
            db.collection("alerts").document(alert.id)
                .delete()
        }
    }

    override fun getItemCount(): Int = alertList.size
}