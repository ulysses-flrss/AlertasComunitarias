package com.example.alertascomunitarias.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.admin.UserItem

class AdminUserAdapter(
    private val userList: MutableList<UserItem>,
    private val onEditClick: (UserItem) -> Unit,
    private val onDeleteClick: (UserItem) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val email: TextView = view.findViewById(R.id.tvEmail)
        val phone: TextView = view.findViewById(R.id.tvPhone)
        val role: TextView = view.findViewById(R.id.tvRole)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]

        holder.name.text = user.name
        holder.email.text = user.email
        holder.phone.text = "Tel: ${user.phone}"
        holder.role.text = "Rol: ${user.role}"

        // 🔴 ELIMINAR
        holder.btnDelete.setOnClickListener {
            onDeleteClick(user)
        }

        // ✏️ EDITAR
        holder.btnEdit.setOnClickListener {
            onEditClick(user)
        }
    }
}