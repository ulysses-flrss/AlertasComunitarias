package com.example.alertascomunitarias.admin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvTotalAlerts: TextView
    private lateinit var tvActiveAlerts: TextView
    private lateinit var tvResolvedAlerts: TextView

    private lateinit var tvTotalUsers: TextView
    private lateinit var tvAdminUsers: TextView
    private lateinit var tvCitizenUsers: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // TextViews
        tvTotalAlerts = findViewById(R.id.tvTotalAlerts)
        tvActiveAlerts = findViewById(R.id.tvActiveAlerts)
        tvResolvedAlerts = findViewById(R.id.tvResolvedAlerts)


        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvAdminUsers = findViewById(R.id.tvAdminUsers)
        tvCitizenUsers = findViewById(R.id.tvCitizenUsers)

        loadDashboard()

        // MENÚ (NO TOCAR)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.mapView

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> true
                R.id.nav_GestionAlertas -> {
                    val intent = Intent(this, AdminAlertsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_GestionUsuarios -> {
                    val intent = Intent(this, AdminUsersActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.menu_cerrar_sesion -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadDashboard() {

        // 🔴 ALERTAS
        db.collection("alerts").get().addOnSuccessListener { result ->

            val total = result.size()
            var active = 0
            var resolved = 0
            var fire = 0

            for (doc in result) {
                val status = doc.getString("status") ?: ""
                val category = doc.getString("category") ?: ""

                if (status == "active") active++
                if (status == "resolved") resolved++
                if (category.lowercase().contains("incendio")) fire++
            }

            tvTotalAlerts.text = total.toString()
            tvActiveAlerts.text = active.toString()
            tvResolvedAlerts.text = resolved.toString()

        }

        // 👥 USUARIOS
        db.collection("users").get().addOnSuccessListener { result ->

            val total = result.size()
            var admin = 0
            var citizen = 0

            for (doc in result) {
                val role = doc.getString("role") ?: ""

                if (role == "administrador") admin++
                if (role == "ciudadano") citizen++
            }

            tvTotalUsers.text = total.toString()
            tvAdminUsers.text = admin.toString()
            tvCitizenUsers.text = citizen.toString()
        }
    }
}