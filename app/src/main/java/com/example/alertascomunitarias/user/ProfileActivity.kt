package com.example.alertascomunitarias.user

import com.example.alertascomunitarias.R
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.EditProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var alertList: MutableList<AlertItem>

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvNotifs: TextView
    private lateinit var tvRadius: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvName = findViewById(R.id.tvDisplayProfileName)
        tvPhone = findViewById(R.id.tvDisplayProfilePhone)
        tvNotifs = findViewById(R.id.tvDisplayNotifications)
        tvRadius = findViewById(R.id.tvDisplayRadius)

        rvAlerts = findViewById<RecyclerView>(R.id.rvMyAlerts)
        rvAlerts.layoutManager = LinearLayoutManager(this)
        alertList = mutableListOf()

        // Botón para ir a editar
        val btnGoToEdit = findViewById<Button>(R.id.btnGoToEdit)
        btnGoToEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Lógica para cerrar sesión
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, com.example.alertascomunitarias.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Navegación Inferior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_map -> {
                    val intent = Intent(this, MapHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_feed -> {
                    val intent = Intent(this, AlertFeedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que la burbuja esté en Perfil
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.menu.findItem(R.id.nav_profile).isChecked = true

        // Cargar/Actualizar datos desde Firebase cada vez que se muestra la pantalla
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvName.text = currentUser.displayName ?: "Usuario Sin Nombre"

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val phone = document.getString("phone")
                        tvPhone.text = if (phone.isNullOrEmpty()) "No registrado" else phone

                        val notifsEnabled = document.getBoolean("notificationsEnabled") ?: false
                        tvNotifs.text = if (notifsEnabled) "Notificaciones: Activadas" else "Notificaciones: Desactivadas"

                        val radius = document.getLong("alertRadiusKm")?.toInt() ?: 5
                        tvRadius.text = "Radio de alerta: $radius km"
                    } else {
                        // Valores por defecto si el documento no existe aún
                        tvPhone.text = "No registrado"
                        tvNotifs.text = "Notificaciones: Desactivadas"
                        tvRadius.text = "Radio de alerta: 5 km"
                    }
                }
        }
        loadUserAlerts()
    }

    private fun loadUserAlerts() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("alerts")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documents ->
                alertList.clear() // Limpiar la lista antes de volver a llenarla

                for (doc in documents) {
                    val alert = AlertItem(
                        id = doc.id,
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                    alertList.add(alert)
                }

                // 🔥 AQUÍ CONECTAMOS EL ADAPTADOR 🔥
                val adapter = AlertAdapter(alertList) { alertaSeleccionada ->
                    // Este bloque de código se ejecutará cuando el usuario toque una alerta de la lista
                    android.widget.Toast.makeText(
                        this,
                        "Tocaste la alerta: ${alertaSeleccionada.category}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                rvAlerts.adapter = adapter // Asignamos el adaptador al RecyclerView
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Error al cargar alertas", android.widget.Toast.LENGTH_SHORT).show()
            }
    }



    data class AlertItem(
        val id: String = "",
        val category: String = "",
        val description: String = "",
        val status: String = ""
    )
}