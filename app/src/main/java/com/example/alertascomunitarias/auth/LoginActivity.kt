package com.example.alertascomunitarias.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.admin.AdminActivity
import com.example.alertascomunitarias.user.MapHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita el diseño de borde a borde
        setContentView(R.layout.activity_login)

        // 🔥 LÓGICA PARA EL TECLADO (Edge-to-Edge Insets)
        val mainView = findViewById<ScrollView>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            // Aplica el padding para evitar que el teclado o las barras tapen el contenido
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializamos las instancias de Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazamos las vistas
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 1. Validación de campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validación local de formato de correo
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "El formato del correo no es válido"
                return@setOnClickListener
            }

            // 3. Intento de inicio de sesión con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val uid = auth.currentUser?.uid

                        if (uid != null) {
                            // 🕵️‍♂️ Consultar a Firestore para verificar si existe y ver su rol
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { document ->

                                    if (document.exists()) {
                                        // ✅ El usuario es válido, verificamos su rol
                                        val rol = document.getString("role") ?: "ciudadano"

                                        if (rol == "administrador") {
                                            startActivity(Intent(this, AdminActivity::class.java))
                                        } else {
                                            startActivity(Intent(this, MapHomeActivity::class.java))
                                        }
                                        finish() // Cerramos LoginActivity

                                    } else {
                                        // ❌ EL TRUCO: El documento no existe (fue eliminado por el admin)
                                        auth.signOut() // Lo sacamos inmediatamente de Auth
                                        Toast.makeText(this, "Acceso denegado. Esta cuenta ha sido inhabilitada o eliminada.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener {
                                    // Error de conexión al intentar leer Firestore
                                    auth.signOut()
                                    Toast.makeText(this, "Error de conexión al verificar la cuenta.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // 4. Manejo detallado de errores de Firebase Auth
                        val exception = task.exception
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                Toast.makeText(this, "Esta cuenta no existe. Por favor regístrate.", Toast.LENGTH_LONG).show()
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                // Recuerda: Si la protección contra enumeración está activa en Firebase,
                                // este error saltará tanto para contraseñas malas como para cuentas que no existen.
                                Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this, "Error de conexión: ${exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        }

        // Navegación a la pantalla de registro
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}