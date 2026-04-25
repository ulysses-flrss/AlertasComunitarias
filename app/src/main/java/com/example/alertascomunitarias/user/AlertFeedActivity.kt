package com.example.alertascomunitarias.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.DocumentChange // 🔥 NUEVO IMPORT
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class AlertFeedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvFeedAlerts: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var feedList: MutableList<ProfileActivity.AlertItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_feed)

        db = FirebaseFirestore.getInstance()

        rvFeedAlerts = findViewById(R.id.rvFeedAlerts)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)

        rvFeedAlerts.layoutManager = LinearLayoutManager(this)
        feedList = mutableListOf()

        setupBottomNavigation()
        setupFilter()
        loadCommunityAlerts()
    }

    private fun setupFilter() {
        val categorias = arrayOf("Todas", "Robo / Asalto", "Incendio", "Accidente de Tránsito", "Actividad Sospechosa", "Emergencia Médica", "Mascota Perdida")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)

        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterFeed)
        dropdown.setAdapter(adapter)

        // Por defecto seleccionamos "Todas"
        dropdown.setText(categorias[0], false)

        dropdown.setOnItemClickListener { parent, view, position, id ->
            val categoriaSeleccionada = categorias[position]
            loadCommunityAlerts(categoriaSeleccionada)
        }
    }

    private fun loadCommunityAlerts(categoriaFiltro: String = "Todas") {

        // 🔥 Variable para controlar si es la carga inicial o una alerta nueva
        var esPrimeraCarga = true

        // 1. Preparamos la consulta base
        var query: Query = db.collection("alerts").whereEqualTo("status", "active")

        // 2. Aplicamos filtro si lo hay
        if (categoriaFiltro != "Todas") {
            query = query.whereEqualTo("category", categoriaFiltro)
        }

        // 3. Ordenamos y escuchamos en tiempo real
        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.e("FirebaseError", "Error de Firestore: ", e)
                    Toast.makeText(this, "Falta un Índice. Revisa la pestaña Logcat.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                // 🔥 DETECTOR DE NUEVAS ALERTAS EN TIEMPO REAL
                if (!esPrimeraCarga && snapshots != null) {
                    // Revisamos exactamente qué cambió en la base de datos
                    for (cambio in snapshots.documentChanges) {
                        // Si el cambio es de tipo "AÑADIDO" (Una nueva alerta)
                        if (cambio.type == DocumentChange.Type.ADDED) {
                            Toast.makeText(this, "🚨 ¡NUEVA ALERTA REPORTADA!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                esPrimeraCarga = false // Marcamos que la carga inicial ya pasó

                feedList.clear()

                // LÓGICA VISUAL DE ESTADO VACÍO
                if (snapshots != null && snapshots.isEmpty) {
                    rvFeedAlerts.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                } else {
                    rvFeedAlerts.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE

                    for (doc in snapshots!!) {
                        // EXTRAEMOS Y FORMATEAMOS LA FECHA
                        val timestamp = doc.getTimestamp("timestamp")
                        val dateString = if (timestamp != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            sdf.format(timestamp.toDate())
                        } else {
                            "Fecha desconocida"
                        }

                        // Creamos el objeto Alerta
                        val alert = ProfileActivity.AlertItem(
                            id = doc.id,
                            category = doc.getString("category") ?: "Alerta",
                            description = doc.getString("description") ?: "Sin detalles.",
                            status = "Activo",
                            date = dateString,
                            userName = doc.getString("name") ?: "Anónimo"
                        )
                        feedList.add(alert)
                    }
                }

                // ACTUALIZAMOS EL ADAPTADOR
                val adapter = AlertAdapter(feedList) { alertaTocada ->
                    mostrarDetallesAlerta(alertaTocada.category, alertaTocada.description, alertaTocada.userName)
                }
                rvFeedAlerts.adapter = adapter
            }
    }

    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?, userName: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)

        val tvCategory = view.findViewById<TextView>(R.id.tvSheetCategory)
        val tvReporter = view.findViewById<TextView>(R.id.tvSheetReporter)
        val tvDescription = view.findViewById<TextView>(R.id.tvSheetDescription)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrarSheet)

        tvCategory.text = categoria
        tvReporter.text = if (userName.isNullOrEmpty()) "Anónimo" else userName
        tvDescription.text = if (descripcion.isNullOrEmpty()) "Sin detalles adicionales." else descripcion

        btnCerrar.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
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