package com.example.alertascomunitarias

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alertascomunitarias.admin.AdminActivity
import com.example.alertascomunitarias.auth.LoginActivity
import com.example.alertascomunitarias.user.MapHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // 🔥 NUEVO IMPORT
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // 🔥 NUEVA VARIABLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inicializamos las instancias de Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // 🔥 INICIALIZAR FIRESTORE

        // Usamos una corrutina para hacer una pausa visual de 2 segundos
        lifecycleScope.launch {
            delay(2000) // 2000 milisegundos = 2 segundos
            checkUserSession()
        }
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario tiene sesión iniciada, pero necesitamos verificar su rol
            val uid = currentUser.uid

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val rol = document.getString("role") ?: "ciudadano"

                    if (rol == "administrador") {
                        // Es administrador, lo enviamos a su panel
                        startActivity(Intent(this@SplashActivity, AdminActivity::class.java))
                    } else {
                        // Es ciudadano, lo enviamos al mapa
                        startActivity(Intent(this@SplashActivity, MapHomeActivity::class.java))
                    }

                    // 🔥 Importante: Destruimos el Splash aquí, DESPUÉS de saber a dónde ir
                    finish()
                }
                .addOnFailureListener {
                    // Si el internet falla justo en este milisegundo, lo mandamos al mapa
                    // para no dejarlo atrapado en la pantalla de carga.
                    Toast.makeText(this, "Error de red. Entrando en modo ciudadano.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SplashActivity, MapHomeActivity::class.java))
                    finish()
                }

        } else {
            // No hay sesión, redirigir a iniciar sesión
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)

            // Destruimos el Splash
            finish()
        }
    }
}