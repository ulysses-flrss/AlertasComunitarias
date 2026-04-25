package com.example.alertascomunitarias.admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.alertascomunitarias.R
import com.google.firebase.firestore.FirebaseFirestore

class EditUserActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnUpdate: Button

    private val db = FirebaseFirestore.getInstance()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        // Referencias
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnUpdate = findViewById(R.id.btnUpdate)

        // Spinner roles
        val roles = resources.getStringArray(R.array.roles_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // 🔥 Recibir datos
        userId = intent.getStringExtra("id") ?: ""
        etName.setText(intent.getStringExtra("name"))
        etEmail.setText(intent.getStringExtra("email"))
        etPhone.setText(intent.getStringExtra("phone"))

        val role = intent.getStringExtra("role") ?: "ciudadano"
        spinnerRole.setSelection(roles.indexOf(role))

        // 🔥 Actualizar
        btnUpdate.setOnClickListener {
            updateUser()
        }
    }

    private fun updateUser() {
        val updatedData = hashMapOf(
            "name" to etName.text.toString(),
            "email" to etEmail.text.toString(),
            "phone" to etPhone.text.toString(),
            "role" to spinnerRole.selectedItem.toString()
        )

        db.collection("users").document(userId)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }
}