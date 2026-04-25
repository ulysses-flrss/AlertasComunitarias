package com.example.alertascomunitarias.user

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
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
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AlertFeedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvFeedAlerts: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var feedList: MutableList<ProfileActivity.AlertItem>
    private lateinit var tvRadiusIndicator: TextView

    // Variables geográficas
    private var miLatitud: Double = 0.0
    private var miLongitud: Double = 0.0
    private var miRadioKm: Double = 5.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_feed)

        db = FirebaseFirestore.getInstance()
        rvFeedAlerts = findViewById(R.id.rvFeedAlerts)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvRadiusIndicator = findViewById(R.id.tvRadiusIndicator)

        rvFeedAlerts.layoutManager = LinearLayoutManager(this)
        feedList = mutableListOf()

        setupBottomNavigation()
        setupFilter()

        inicializarFiltroGeografico()
    }

    private fun inicializarFiltroGeografico() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                miRadioKm = doc.getLong("alertRadiusKm")?.toDouble() ?: 5.0
            }
            obtenerUbicacionActual()
        }.addOnFailureListener {
            obtenerUbicacionActual()
        }
    }

    private fun obtenerUbicacionActual() {
        var gpsExitoso = false

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                miLatitud = location.latitude
                miLongitud = location.longitude
                gpsExitoso = true
            }
        }

        if (gpsExitoso) {
            val radioInt = miRadioKm.toInt()
            tvRadiusIndicator.text = "📍 Mostrando alertas a $radioInt km de tu ubicación"
        } else {
            tvRadiusIndicator.text = "🌍 Mostrando todas las alertas (GPS no disponible)"
        }

        loadCommunityAlerts("Todas")
    }

    // Fórmula de Haversine para distancias exactas
    private fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierra * c
    }

    private fun setupFilter() {
        val categorias = arrayOf("Todas", "Robo / Asalto", "Incendio", "Accidente de Tránsito", "Actividad Sospechosa", "Emergencia Médica", "Mascota Perdida")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterFeed)
        dropdown.setAdapter(adapter)
        dropdown.setText(categorias[0], false)
        dropdown.setOnItemClickListener { _, _, position, _ -> loadCommunityAlerts(categorias[position]) }
    }

    private fun loadCommunityAlerts(categoriaFiltro: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        var esPrimeraCarga = true
        var query: Query = db.collection("alerts").whereEqualTo("status", "active")

        if (categoriaFiltro != "Todas") query = query.whereEqualTo("category", categoriaFiltro)

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (!esPrimeraCarga && snapshots != null) {
                    for (cambio in snapshots.documentChanges) {
                        if (cambio.type == DocumentChange.Type.ADDED) {
                            Toast.makeText(this, "🚨 ¡NUEVA ALERTA CERCANA!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                esPrimeraCarga = false
                feedList.clear()

                if (snapshots != null) {
                    for (doc in snapshots) {

                        val alertLat = doc.getDouble("latitude") ?: 0.0
                        val alertLon = doc.getDouble("longitude") ?: 0.0

                        var estaCerca = true
                        var textoDistancia = ""

                        // 🔥 Validar si el usuario y la alerta tienen ubicación
                        if (miLatitud != 0.0 && miLongitud != 0.0 && alertLat != 0.0) {
                            val distancia = calcularDistanciaKm(miLatitud, miLongitud, alertLat, alertLon)
                            estaCerca = distancia <= miRadioKm

                            // Redondeamos la distancia a 1 decimal (Ej: "1.5 km")
                            textoDistancia = String.format(Locale.getDefault(), "%.1f km", distancia)
                        }

                        if (estaCerca) {
                            val timestamp = doc.getTimestamp("timestamp")
                            val dateString = if (timestamp != null) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp.toDate()) else "Sin fecha"

                            val reporterName = doc.getString("name") ?: doc.getString("userName") ?: doc.getString("usuario") ?: "Anónimo"

                            val alert = ProfileActivity.AlertItem(
                                id = doc.id,
                                category = doc.getString("category") ?: "Alerta",
                                description = doc.getString("description") ?: "",
                                status = "Activo",
                                date = dateString,
                                userName = reporterName,
                                userId = doc.getString("userId") ?: "",
                                distance = textoDistancia // 🔥 ENVIAMOS LA DISTANCIA FORMATEADA
                            )
                            feedList.add(alert)
                        }
                    }
                }

                if (feedList.isEmpty()) {
                    rvFeedAlerts.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE
                } else {
                    rvFeedAlerts.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                }

                rvFeedAlerts.adapter = AlertAdapter(
                    feedList,
                    currentUserId = currentUserId,
                    onAlertClick = { a -> mostrarDetallesAlerta(a.category, a.description, a.userName) },
                    onResolveClick = { a -> resolverAlerta(a.id) }
                )
            }
    }

    private fun resolverAlerta(alertId: String) {
        db.collection("alerts").document(alertId).update("status", "resuelto")
            .addOnSuccessListener { Toast.makeText(this, "Alerta resuelta", Toast.LENGTH_SHORT).show() }
    }

    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?, userName: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)
        view.findViewById<TextView>(R.id.tvSheetCategory).text = categoria
        view.findViewById<TextView>(R.id.tvSheetReporter).text = if (userName.isNullOrBlank() || userName == "null") "Anónimo" else userName
        view.findViewById<TextView>(R.id.tvSheetDescription).text = descripcion ?: "Sin detalles."
        view.findViewById<Button>(R.id.btnCerrarSheet).setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_feed
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_feed -> true
                R.id.nav_map -> { startActivity(Intent(this, MapHomeActivity::class.java)); false }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); false }
                else -> false
            }
        }
    }
}