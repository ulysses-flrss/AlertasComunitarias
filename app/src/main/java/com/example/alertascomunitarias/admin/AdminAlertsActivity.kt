package com.example.alertascomunitarias.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder // 🔥 Diálogo moderno
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminAlertsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var alertList: MutableList<AdminAlertItem>
    private lateinit var adapter: AdminAlertAdapter
    private lateinit var spinner: Spinner

    // 🔥 Variables para el estado vacío
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyStateText: TextView

    private var selectedCategory: String = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_alerts)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rvAdminAlerts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinner = findViewById(R.id.spinnerCategory)
        layoutEmptyState = findViewById(R.id.layoutEmptyStateAdmin)
        tvEmptyStateText = findViewById(R.id.tvEmptyStateText)

        alertList = mutableListOf()

        setupSpinner()
        loadAlerts()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_GestionAlertas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
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
        // 🔥 Usando el diseño Material que implementamos antes
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, salir") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
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
            "Accidente de Tránsito",
            "Emergencia Médica",
            "Actividad Sospechosa",
            "Mascota Perdida"
        )

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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

                // 1. Manejo de Errores Reales (Ej: Falta de índices)
                if (e != null) {
                    Log.e("FirebaseError", "¡Error en la consulta!: ", e)
                    Toast.makeText(this, "Falta índice en Firebase. Revisa el Logcat.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                alertList.clear()

                // 2. Manejo de "Estado Vacío" (0 Resultados)
                if (snapshots != null && snapshots.isEmpty) {
                    recyclerView.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE

                    // Texto dinámico amigable
                    if (selectedCategory == "Todas") {
                        tvEmptyStateText.text = "No hay ninguna alerta registrada en el sistema."
                    } else {
                        tvEmptyStateText.text = "No hay alertas registradas en la categoría:\n$selectedCategory"
                    }
                } else {
                    // 3. Hay resultados, mostramos la lista
                    recyclerView.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE

                    snapshots?.forEach { doc ->
                        val alert = AdminAlertItem(
                            id = doc.id,
                            category = doc.getString("category") ?: "Sin categoría",
                            description = doc.getString("description") ?: "",
                            userName = doc.getString("userName") ?: doc.getString("name") ?: doc.getString("usuario") ?: "Anónimo",
                            status = doc.getString("status") ?: "active"
                        )
                        alertList.add(alert)
                    }
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