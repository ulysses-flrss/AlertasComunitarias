package com.example.alertascomunitarias.admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddUserActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText // 🔥 NUEVO
    private lateinit var etPhone: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // 🔥 NUEVO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword) // 🔥 NUEVO
        etPhone = findViewById(R.id.etPhone)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnSave = findViewById(R.id.btnSaveUser)

        // Spinner roles
        val roles = resources.getStringArray(R.array.roles_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        btnSave.setOnClickListener {
            saveUser()
        }
    }

    private fun saveUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val role = spinnerRole.selectedItem.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nombre, correo y contraseña son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Desactivamos el botón para evitar dobles clics
        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        // 1. Crear el usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // 2. Crear el documento en Firestore con el MISMO UID y las preferencias
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to role,
                    "alertRadiusKm" to 5, // 🔥 Preferencia inicial
                    "notificationsEnabled" to true // 🔥 Preferencia inicial
                )

                db.collection("users").document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                        finish() // Vuelve atrás
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                        btnSave.text = "Guardar Usuario"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error de Auth: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "Guardar Usuario"
            }
    }
}