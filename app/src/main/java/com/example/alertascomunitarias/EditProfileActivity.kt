package com.example.alertascomunitarias

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEditName = findViewById<EditText>(R.id.etEditName)
        val etEditPhone = findViewById<EditText>(R.id.etEditPhone)
        val switchEditNotifications = findViewById<SwitchCompat>(R.id.switchEditNotifications)
        val tvEditRadiusLabel = findViewById<TextView>(R.id.tvEditRadiusLabel)
        val seekBarEditRadius = findViewById<SeekBar>(R.id.seekBarEditRadius)
        val btnSaveEdits = findViewById<Button>(R.id.btnSaveEdits)

        val currentUser = auth.currentUser

        // 1. Rellenar el formulario con los datos actuales
        if (currentUser != null) {
            etEditName.setText(currentUser.displayName)

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        etEditPhone.setText(document.getString("phone") ?: "")
                        switchEditNotifications.isChecked = document.getBoolean("notificationsEnabled") ?: false

                        val radius = document.getLong("alertRadiusKm")?.toInt() ?: 5
                        seekBarEditRadius.progress = radius
                        tvEditRadiusLabel.text = "Radio de alerta: $radius km"
                    }
                }
        }

        // Actualizar la etiqueta del Slider
        seekBarEditRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvEditRadiusLabel.text = "Radio de alerta: $progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 2. Guardar los cambios
        btnSaveEdits.setOnClickListener {
            if (currentUser == null) return@setOnClickListener

            val newName = etEditName.text.toString().trim()
            val newPhone = etEditPhone.text.toString().trim()

            // A. Actualizar nombre en Firebase Auth
            if (newName.isNotEmpty() && newName != currentUser.displayName) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                currentUser.updateProfile(profileUpdates)
            }

            // B. Actualizar el resto en Firestore
            val userData = hashMapOf(
                "phone" to newPhone,
                "notificationsEnabled" to switchEditNotifications.isChecked,
                "alertRadiusKm" to seekBarEditRadius.progress
            )

            // Usamos SetOptions.merge() para no borrar otros campos que el usuario pudiera tener
            db.collection("users").document(currentUser.uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish() // Cierra esta pantalla y regresa automáticamente al ProfileActivity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}