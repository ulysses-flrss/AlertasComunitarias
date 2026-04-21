package com.example.alertascomunitarias.user

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.alertascomunitarias.R
class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etProfileName = findViewById<EditText>(R.id.etProfileName)
        val etProfilePhone = findViewById<EditText>(R.id.etProfilePhone)
        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        val tvRadiusLabel = findViewById<TextView>(R.id.tvRadiusLabel)
        val seekBarRadius = findViewById<SeekBar>(R.id.seekBarRadius)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val currentUser = auth.currentUser

        // Cargar datos actuales del usuario si existen
        if (currentUser != null) {
            etProfileName.setText(currentUser.displayName)

            // Consultar el resto de los datos en Firestore
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        etProfilePhone.setText(document.getString("phone") ?: "")
                        switchNotifications.isChecked = document.getBoolean("notificationsEnabled") ?: false

                        val radius = document.getLong("alertRadiusKm")?.toInt() ?: 5
                        seekBarRadius.progress = radius
                        tvRadiusLabel.text = "Radio de alerta: $radius km"
                    }
                }
        }

        // Actualizar el texto mientras se mueve la barra del radio
        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvRadiusLabel.text = "Radio de alerta: $progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Guardar cambios en Firestore
        btnSaveProfile.setOnClickListener {
            if (currentUser == null) return@setOnClickListener

            val userData = hashMapOf(
                "name" to etProfileName.text.toString().trim(),
                "phone" to etProfilePhone.text.toString().trim(),
                "notificationsEnabled" to switchNotifications.isChecked,
                "alertRadiusKm" to seekBarRadius.progress
            )

            db.collection("users").document(currentUser.uid)
                .set(userData) // set() crea o sobreescribe el documento
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish() // Regresar al mapa
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Lógica para cerrar sesión
        btnLogout.setOnClickListener {
            auth.signOut()
            // Redirigir al Login y limpiar el historial de pantallas
            val intent = Intent(this, com.example.alertascomunitarias.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile // Marcamos "Perfil" como seleccionado

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true // Ya estamos aquí
                R.id.nav_map -> {
                    val intent = Intent(this, MapHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }
}