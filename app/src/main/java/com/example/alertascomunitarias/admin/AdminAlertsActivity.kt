package com.example.alertascomunitarias.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminAlertsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var alertList: MutableList<AdminAlertItem>
    private lateinit var adapter: AdminAlertAdapter
    private lateinit var spinner: Spinner

    private var selectedCategory: String = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_alerts)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rvAdminAlerts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinner = findViewById(R.id.spinnerCategory)

        alertList = mutableListOf()

        setupSpinner()
        loadAlerts()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.mapView

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    val intent = Intent(this, AdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_GestionAlertas -> true
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

    private fun setupSpinner() {
        val categories = listOf(
            "Todas",
            "Incendio",
            "Robo / Asalto",
            "Accidente",
            "Emergencia Médica",
            "Actividad Sospechosa"
        )

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                selectedCategory = categories[position]
                loadAlerts()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadAlerts() {

        var query: Query = db.collection("alerts")

        if (selectedCategory != "Todas") {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.e("AdminAlerts", "Error", e)
                    Toast.makeText(this, "Error cargando alertas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                alertList.clear()

                snapshots?.forEach { doc ->   // 👈 AQUÍ ya no hay problema

                    val alert = AdminAlertItem(
                        id = doc.id,
                        category = doc.getString("category") ?: "Sin categoría",
                        description = doc.getString("description") ?: "",
                        userName = doc.getString("userName") ?: "Anónimo",
                        status = doc.getString("status") ?: "active"
                    )

                    alertList.add(alert)
                }

                if (!::adapter.isInitialized) {
                    adapter = AdminAlertAdapter(alertList)
                    recyclerView.adapter = adapter
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
    }
}