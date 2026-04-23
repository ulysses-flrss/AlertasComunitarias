package com.example.alertascomunitarias.user

import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AlertFeedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvFeedAlerts: RecyclerView
    private lateinit var feedList: MutableList<ProfileActivity.AlertItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_feed)

        db = FirebaseFirestore.getInstance()

        rvFeedAlerts = findViewById(R.id.rvFeedAlerts)
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

        dropdown.setText(categorias[0], false)

        dropdown.setOnItemClickListener { parent, view, position, id ->
            val categoriaSeleccionada = categorias[position]
            // Llamamos a la función para que recargue el muro con el filtro aplicado
            loadCommunityAlerts(categoriaSeleccionada)
        }
    }

    // 🔥 Ahora recibe el parámetro con valor por defecto "Todas"
    private fun loadCommunityAlerts(categoriaFiltro: String = "Todas") {

        // 1. Preparamos la consulta base (solo alertas activas)
        var query: Query = db.collection("alerts").whereEqualTo("status", "active")

        // 2. Si hay un filtro, se lo agregamos a la consulta
        if (categoriaFiltro != "Todas") {
            query = query.whereEqualTo("category", categoriaFiltro)
        }

        // 3. Ordenamos por fecha y escuchamos los cambios en tiempo real
        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                // 🔥 CAPTURAMOS EL ERROR (Falta de Índice)
                if (e != null) {
                    Log.e("FirebaseError", "Error de Firestore: ", e)
                    Toast.makeText(this, "Falta un Índice. Revisa la pestaña Logcat.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                // Limpiamos la lista vieja para no duplicar datos
                feedList.clear()

                // 🔥 MANEJAMOS EL CASO DONDE NO HAY ALERTAS
                if (snapshots != null && snapshots.isEmpty) {
                    Toast.makeText(this, "No hay alertas en esta categoría", Toast.LENGTH_SHORT).show()
                } else {
                    // Si hay datos, los convertimos y los agregamos a la lista
                    for (doc in snapshots!!) {
                        val alert = ProfileActivity.AlertItem(
                            id = doc.id,
                            category = doc.getString("category") ?: "Alerta",
                            description = "Reportado por: ${doc.getString("userName") ?: "Anónimo"}\n" +
                                    (doc.getString("description") ?: "Sin detalles."),
                            status = "Activo"
                        )
                        feedList.add(alert)
                    }
                }

                // 🔥 SIEMPRE ACTUALIZAMOS EL ADAPTADOR AL FINAL
                // Al tocar una tarjeta, llamamos a mostrarDetallesAlerta (el BottomSheet)
                val adapter = AlertAdapter(feedList) { alertaTocada ->
                    mostrarDetallesAlerta(alertaTocada.category, alertaTocada.description)
                }
                rvFeedAlerts.adapter = adapter
            }
    }

    // 🔥 NUEVA FUNCIÓN: Traída desde el Mapa para mostrar la tarjeta emergente
    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)

        // Enlazar los textos del XML
        val tvCategory = view.findViewById<TextView>(R.id.tvSheetCategory)
        val tvDescription = view.findViewById<TextView>(R.id.tvSheetDescription)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrarSheet)

        tvCategory.text = categoria
        tvDescription.text = if (descripcion.isNullOrEmpty()) "Sin detalles adicionales." else descripcion

        // Cerrar la tarjeta al presionar el botón
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