package com.example.alertascomunitarias.user

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.alertascomunitarias.R

class CreateAlertActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var currentLat: Double? = null
    private var currentLon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_alert)

        // Inicializar servicios
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Configurar UI
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSubmitAlert = findViewById<Button>(R.id.btnSubmitAlert)
        val tvLocationStatus = findViewById<TextView>(R.id.tvLocationStatus)
        val pbLocation = findViewById<ProgressBar>(R.id.pbLocation)

        // Poblar el Spinner con las categorías
        val categories = arrayOf("Robo / Asalto", "Accidente de Tránsito", "Incendio", "Actividad Sospechosa", "Emergencia Médica")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter

        // Obtener ubicación al abrir la pantalla
        fetchLocation(btnSubmitAlert, tvLocationStatus, pbLocation)

        btnSubmitAlert.setOnClickListener {
            val category = spinnerCategory.selectedItem.toString()
            val description = etDescription.text.toString().trim()

            if (currentLat != null && currentLon != null) {
                saveAlertToFirebase(category, description)
            } else {
                Toast.makeText(this, "Aún no tenemos tu ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLocation(btnSubmit: Button, statusText: TextView, progressBar: ProgressBar) {
        // Verificamos si tenemos permiso
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            finish() // Cierra la actividad si no hay permiso
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude

                // Actualizamos la UI
                progressBar.visibility = View.GONE
                statusText.text = "Ubicación obtenida con precisión"
                statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                btnSubmit.isEnabled = true // Activamos el botón
            } else {
                statusText.text = "Error al obtener ubicación. Enciende el GPS."
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveAlertToFirebase(category: String, description: String) {
        val user = auth.currentUser
        if (user == null) return

        // Creamos el documento que se enviará a la colección "alerts"
        val alertData = hashMapOf(
            "userId" to user.uid,
            "userName" to (user.displayName ?: "Ciudadano Anónimo"),
            "category" to category,
            "description" to description,
            "latitude" to currentLat,
            "longitude" to currentLon,
            "timestamp" to FieldValue.serverTimestamp(), // Toma la hora del servidor de Google
            "status" to "active"
        )

        db.collection("alerts")
            .add(alertData)
            .addOnSuccessListener {
                Toast.makeText(this, "Alerta publicada exitosamente", Toast.LENGTH_LONG).show()
                finish() // Cierra la pantalla y devuelve al usuario al Mapa
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}