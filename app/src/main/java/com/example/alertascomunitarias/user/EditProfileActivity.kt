package com.example.alertascomunitarias.user

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas exactamente como están en tu XML
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var switchNotifs: MaterialSwitch
    private lateinit var seekBarRadius: SeekBar
    private lateinit var tvRadiusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazamos con los IDs correctos
        etName = findViewById(R.id.etEditName)
        etPhone = findViewById(R.id.etEditPhone)
        switchNotifs = findViewById(R.id.switchEditNotifications)
        seekBarRadius = findViewById(R.id.seekBarEditRadius)
        tvRadiusLabel = findViewById(R.id.tvEditRadiusLabel)

        // El ID correcto de tu botón de guardar
        val btnSave = findViewById<Button>(R.id.btnSaveEdits)

        // 🔥 NUEVA LÓGICA: Actualizar el texto mientras mueves la barrita
        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Actualiza el texto en tiempo real
                tvRadiusLabel.text = "Radio de alerta: $progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 1. Cargar datos
        loadUserData()

        // 2. Guardar datos
        btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        etName.setText(currentUser.displayName ?: "")

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    // El fix de UX para el teléfono
                    val phone = document.getString("phone")
                    if (phone.isNullOrEmpty() || phone.uppercase() == "NO ESPECIFICADO" || phone == "No registrado") {
                        etPhone.setText("")
                    } else {
                        etPhone.setText(phone)
                    }

                    // Cargar estado de la palanca de notificaciones
                    val notifsEnabled = document.getBoolean("notificationsEnabled") ?: false
                    switchNotifs.isChecked = notifsEnabled

                    // 🔥 Cargar el radio al SeekBar y al texto
                    val radius = document.getLong("alertRadiusKm")?.toInt() ?: 5
                    seekBarRadius.progress = radius
                    tvRadiusLabel.text = "Radio de alerta: $radius km"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val newName = etName.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newNotifs = switchNotifs.isChecked
        val newRadius = seekBarRadius.progress // 🔥 Obtenemos el valor de la barrita

        if (newName.isEmpty()) {
            etName.error = "El nombre no puede estar vacío"
            return
        }

        Toast.makeText(this, "Guardando...", Toast.LENGTH_SHORT).show()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        currentUser.updateProfile(profileUpdates).addOnCompleteListener { taskAuth ->
            if (taskAuth.isSuccessful) {

                val updates = hashMapOf<String, Any>(
                    "name" to newName,
                    "phone" to newPhone,
                    "notificationsEnabled" to newNotifs,
                    "alertRadiusKm" to newRadius
                )

                db.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        db.collection("users").document(uid).set(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Perfil creado con éxito", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
            } else {
                Toast.makeText(this, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
            }
        }
    }
}