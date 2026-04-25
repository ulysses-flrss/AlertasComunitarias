package com.example.alertascomunitarias.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog

class AdminAlertAdapter(
    private val alertList: List<AdminAlertItem>
) : RecyclerView.Adapter<AdminAlertAdapter.AdminViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvAdminCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvAdminDescription)
        val tvUser: TextView = itemView.findViewById(R.id.tvAdminUser)
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

        // Eliminar alerta con confirmación
        holder.btnDelete.setOnClickListener {

            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Eliminar alerta")
                .setMessage("¿Estás seguro de que deseas eliminar esta alerta?")
                .setPositiveButton("Sí") { _, _ ->
                    db.collection("alerts").document(alert.id)
                        .delete()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun getItemCount(): Int = alertList.size
}