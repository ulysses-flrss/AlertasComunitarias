package com.example.alertascomunitarias.user

import com.example.alertascomunitarias.R

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapHomeActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var db: FirebaseFirestore
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración de osmdroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_map_home)

        // 2. Inicializar variables
        db = FirebaseFirestore.getInstance()
        map = findViewById(R.id.mapView)



        // 3. Configurar Mapa y Permisos
        setupMap()
        requestPermissionsIfNecessary(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

        centerLocation()
        // 4. Botón: Reportar Alerta (Naranja)
        val fabAddAlert = findViewById<FloatingActionButton>(R.id.fabAddAlert)
        fabAddAlert.setOnClickListener {
            val intent = Intent(this@MapHomeActivity, CreateAlertActivity::class.java)
            startActivity(intent)
        }

        // 5. Botón: Centrar GPS (Blanco)
        val fabCenterLocation = findViewById<FloatingActionButton>(R.id.fabCenterLocation)
        fabCenterLocation.setOnClickListener {
            centerLocation()
        }

        // 6. Menú Inferior (Bottom Navigation)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.mapView

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.mapView ->  true
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

        // 7. Escuchar la base de datos
        listenForAlerts()
    }

    private fun centerLocation () {
        if (locationOverlay.myLocation != null) {
            map.controller.animateTo(locationOverlay.myLocation)
            map.controller.setZoom(18.0)
        } else {
            Toast.makeText(this, "Buscando señal GPS...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(16.0)

        // Punto inicial por defecto
        val startPoint = GeoPoint(13.6929, -89.2182)
        mapController.setCenter(startPoint)

        // Capa de ubicación del usuario
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)
    }


    private fun listenForAlerts() {
        db.collection("alerts")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                // Limpiar marcadores antiguos si es necesario (simplificado para este ejemplo)
                for (doc in snapshots!!) {
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lon = doc.getDouble("longitude") ?: 0.0
                    val title = doc.getString("category") ?: "Alerta"
                    val desc = doc.getString("description") ?: ""

                    val marker = Marker(map)
                    marker.position = GeoPoint(lat, lon)
                    marker.title = title
                    marker.snippet = desc
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                }
                map.invalidate() // Refresca el mapa para pintar los íconos
            }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<out String>) {
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    // Ciclo de vida del mapa
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}