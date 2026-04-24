package com.example.alertascomunitarias.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R

class AdminActivity : AppCompatActivity() {

    private lateinit var btnGestionarAlertas: Button
    private lateinit var btnGestionarCategorias: Button
    private lateinit var btnVerUsuarios: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        btnGestionarAlertas = findViewById(R.id.btnGestionarAlertas)
        btnGestionarCategorias = findViewById(R.id.btnGestionarCategorias)
        btnVerUsuarios = findViewById(R.id.btnVerUsuarios)

        // Ir a gestionar alertas
        btnGestionarAlertas.setOnClickListener {
            val intent = Intent(this, AdminAlertsActivity::class.java)
            startActivity(intent)
        }

        // Ir a categorías
        btnGestionarCategorias.setOnClickListener {
            val intent = Intent(this, AdminCategoriesActivity::class.java)
            startActivity(intent)
        }

        // Ir a usuarios
        btnVerUsuarios.setOnClickListener {
            val intent = Intent(this, AdminUsersActivity::class.java)
            startActivity(intent)
        }
    }
}