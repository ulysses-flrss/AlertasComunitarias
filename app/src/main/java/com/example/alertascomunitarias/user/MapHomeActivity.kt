package com.example.alertascomunitarias.user

import com.example.alertascomunitarias.R

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import android.widget.Button
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
        setupFilter()
        requestPermissionsIfNecessary(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))


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

    private fun setupFilter() {
        // 1. Aquí defines todas tus categorías
        val categorias = arrayOf("Todas", "Robo / Asalto", "Incendio", "Accidente de Tránsito", "Actividad Sospechosa", "Emergencia Médica", "Mascota Perdida")

        // 2. Creamos el adaptador que dibujará la lista
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)

        // 3. Conectamos el adaptador con la vista XML
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilter)
        dropdown.setAdapter(adapter)

        // 4. Dejamos "Todas" seleccionada por defecto al abrir la app
        dropdown.setText(categorias[0], false)

        // 5. Escuchamos cuando el usuario elige una opción
        dropdown.setOnItemClickListener { parent, view, position, id ->
            val categoriaSeleccionada = categorias[position]
            // Llamamos a la función que actualiza el mapa
            listenForAlerts(categoriaSeleccionada)
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        val mapController = map.controller
        mapController.setZoom(16.0)

        // Punto inicial por defecto
        val startPoint = GeoPoint(13.6929, -89.2182)
        mapController.setCenter(startPoint)

        // Capa de ubicación del usuario
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        locationOverlay.runOnFirstFix {
            runOnUiThread { centerLocation() }
        }

    }


    private fun listenForAlerts(categoriaFiltro: String = "Todas") {
        // 1. Preparamos la consulta base (solo alertas activas)
        var query = db.collection("alerts").whereEqualTo("status", "active")

        // 2. Si el usuario eligió un filtro en el menú, modificamos la consulta
        if (categoriaFiltro != "Todas") {
            query = query.whereEqualTo("category", categoriaFiltro)
        }

        // 3. Ejecutamos la consulta en tiempo real
        query.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener

            // Limpiamos los marcadores anteriores de la pantalla, pero
            // siempre salvamos la capa [0] que es el GPS del usuario.
            if (map.overlays.size > 1) {
                val locationLayer = map.overlays[0]
                map.overlays.clear()
                map.overlays.add(locationLayer)
            }

            // 4. Dibujamos los nuevos puntos filtrados
            for (doc in snapshots!!) {
                val lat = doc.getDouble("latitude") ?: 0.0
                val lon = doc.getDouble("longitude") ?: 0.0
                val title = doc.getString("category") ?: "Alerta"
                val desc = doc.getString("description") ?: "Sin descripción adicional."

                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = title
                marker.snippet = desc
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Lógica al tocar el marcador
                marker.setOnMarkerClickListener { currentMarker, mapView ->
                    // Mostramos la tarjeta emergente inferior
                    mostrarDetallesAlerta(currentMarker.title, currentMarker.snippet)

                    // Centramos la cámara suavemente en el ícono tocado
                    mapView.controller.animateTo(currentMarker.position)
                    true
                }

                map.overlays.add(marker)
            }

            // Le decimos al mapa que se actualice visualmente
            map.invalidate()
        }
    }

    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)

        // Enlazar los textos del XML con los datos de Firebase
        val tvCategory = view.findViewById<TextView>(R.id.tvSheetCategory)
        val tvDescription = view.findViewById<TextView>(R.id.tvSheetDescription)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrarSheet)

        tvCategory.text = categoria
        tvDescription.text = if (descripcion.isNullOrEmpty()) "El usuario no proporcionó detalles adicionales." else descripcion

        // Cerrar la tarjeta al presionar el botón
        btnCerrar.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
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

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.menu.findItem(R.id.nav_map).isChecked = true
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}