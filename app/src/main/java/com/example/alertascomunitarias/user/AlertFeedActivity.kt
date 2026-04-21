package com.example.alertascomunitarias.user

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.text.category

class AlertFeedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvFeedAlerts: RecyclerView
    private lateinit var feedList: MutableList<ProfileActivity.AlertItem> // Usamos la misma clase de datos que ya tienes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_feed)

        db = FirebaseFirestore.getInstance()

        rvFeedAlerts = findViewById(R.id.rvFeedAlerts)
        rvFeedAlerts.layoutManager = LinearLayoutManager(this)
        feedList = mutableListOf()

        setupBottomNavigation()
        loadCommunityAlerts()
    }

    private fun loadCommunityAlerts() {
        // Traemos todas las alertas activas, ordenadas por la más reciente primero
        db.collection("alerts")
            .whereEqualTo("status", "active")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al cargar el muro", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                feedList.clear()
                for (doc in snapshots!!) {
                    val alert = ProfileActivity.AlertItem(
                        id = doc.id,
                        category = doc.getString("category") ?: "Alerta",
                        // Agregamos el nombre del usuario a la descripción para saber quién lo publicó
                        description = "Reportado por: ${doc.getString("userName") ?: "Anónimo"}\n" +
                                (doc.getString("description") ?: "Sin detalles."),
                        status = "Activo"
                    )
                    feedList.add(alert)
                }

                // Usamos el mismo AlertAdapter de la pantalla de Perfil
                val adapter = AlertAdapter(feedList) { alertaTocada ->
                    Toast.makeText(this, "Categoría: ${alertaTocada.category}", Toast.LENGTH_SHORT).show()
                }
                rvFeedAlerts.adapter = adapter
            }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_feed

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_feed -> true
                R.id.nav_map -> {
                    val intent = Intent(this, MapHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
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
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.menu.findItem(R.id.nav_feed).isChecked = true
    }
}