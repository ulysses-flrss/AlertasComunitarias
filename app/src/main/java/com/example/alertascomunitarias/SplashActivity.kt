package com.example.alertascomunitarias

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alertascomunitarias.auth.LoginActivity
import com.example.alertascomunitarias.user.MapHomeActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    // Variable para instanciar Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Usamos una corrutina para hacer una pausa visual de 2 segundos
        lifecycleScope.launch {
            delay(2000) // 2000 milisegundos = 2 segundos
            checkUserSession()
        }
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario ya tiene sesión iniciada, redirigir al Mapa
            // Nota: MapHomeActivity aún no existe, pero ya dejamos la ruta lista
            val intent = Intent(this@SplashActivity, MapHomeActivity::class.java)
            startActivity(intent)
        } else {
            // No hay sesión, redirigir a iniciar sesión o registrarse
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        // Destruimos el SplashActivity para que el usuario no pueda volver usando el botón "Atrás"
        finish()
    }
}