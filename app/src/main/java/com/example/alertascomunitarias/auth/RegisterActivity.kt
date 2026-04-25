package com.example.alertascomunitarias.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore // 🔥 NUEVO IMPORT

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // 🔥 NUEVA VARIABLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // 🔥 INICIALIZACIÓN DE FIRESTORE

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val etName = findViewById<EditText>(R.id.etRegisterName)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 1. Guardar el nombre en el perfil básico de Firebase Auth
                            val user = auth.currentUser
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener {

                                    // 🔥 2. CREAR EL DOCUMENTO EN LA COLECCIÓN "users"
                                    val userMap = hashMapOf(
                                        "name" to name,
                                        "email" to email,
                                        "role" to "ciudadano", // El rol por defecto
                                        "phone" to "No registrado", // Evita nulos en el perfil
                                        "notificationsEnabled" to true, // Valor por defecto
                                        "alertRadius" to 5 // Valor por defecto en KM
                                    )

                                    db.collection("users").document(user.uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                                            // Aquí podrías redirigir al Mapa
                                            // startActivity(Intent(this, MapHomeActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error al crear datos de perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}