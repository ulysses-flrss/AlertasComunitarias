package com.example.alertascomunitarias.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.user.EditProfileActivity
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var alertList: MutableList<AlertItem>

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvNotifs: TextView
    private lateinit var tvRadius: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazar vistas de perfil
        tvName = findViewById(R.id.tvDisplayProfileName)
        tvPhone = findViewById(R.id.tvDisplayProfilePhone)
        tvNotifs = findViewById(R.id.tvDisplayNotifications)
        tvRadius = findViewById(R.id.tvDisplayRadius)

        // Configurar RecyclerView
        rvAlerts = findViewById(R.id.rvMyAlerts)
        rvAlerts.layoutManager = LinearLayoutManager(this)
        alertList = mutableListOf()

        // Configurar Filtros y Navegación
        setupFilter()
        setupButtons()
        setupBottomNavigation()
    }

    private fun setupButtons() {
        // Botón para ir a editar perfil
        val btnGoToEdit = findViewById<Button>(R.id.btnGoToEdit)
        btnGoToEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Lógica para cerrar sesión
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, com.example.alertascomunitarias.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupFilter() {
        val categorias = arrayOf("Todas", "Robo", "Incendio", "Accidente de Tráfico", "Persona Sospechosa", "Emergencia Médica", "Mascota Perdida")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)

        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterProfile)
        dropdown.setAdapter(adapter)

        val switchHistorial = findViewById<SwitchMaterial>(R.id.switchHistorial)

        // Escuchar cambios en el menú desplegable
        dropdown.setOnItemClickListener { _, _, position, _ ->
            val categoriaSeleccionada = categorias[position]
            loadUserAlerts(categoriaSeleccionada, switchHistorial.isChecked)
        }

        // Escuchar cambios en el switch de historial
        switchHistorial.setOnCheckedChangeListener { _, isChecked ->
            val currentFilter = dropdown.text.toString().ifEmpty { "Todas" }
            loadUserAlerts(currentFilter, isChecked)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_map -> {
                    val intent = Intent(this, MapHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_feed -> {
                    val intent = Intent(this, AlertFeedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar datos de usuario
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvName.text = currentUser.displayName ?: "Usuario"

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val phone = document.getString("phone")
                        tvPhone.text = if (phone.isNullOrEmpty()) "No registrado" else phone

                        val notifsEnabled = document.getBoolean("notificationsEnabled") ?: false
                        tvNotifs.text = if (notifsEnabled) "Notificaciones: Activadas" else "Notificaciones: Desactivadas"

                        val radius = document.getLong("alertRadiusKm")?.toInt() ?: 5
                        tvRadius.text = "Radio de alerta: $radius km"
                    }
                }
        }

        // Recargar alertas con los filtros actuales
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterProfile)
        val switchHistorial = findViewById<SwitchMaterial>(R.id.switchHistorial)
        val currentFilter = dropdown.text.toString().ifEmpty { "Todas" }
        loadUserAlerts(currentFilter, switchHistorial.isChecked)
    }

    private fun loadUserAlerts(categoriaFiltro: String = "Todas", mostrarResueltas: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Consulta base por mi ID
        var query: Query = db.collection("alerts").whereEqualTo("userId", uid)

        // 2. Si el switch está APAGADO, solo mostramos las activas
        if (!mostrarResueltas) {
            query = query.whereEqualTo("status", "active")
        }

        // 3. Filtro por categoría
        if (categoriaFiltro != "Todas") {
            query = query.whereEqualTo("category", categoriaFiltro)
        }

        // 4. Orden cronológico
        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.e("FirebaseError", "Error en Perfil: ", e)
                    // Si sale este mensaje, recuerda crear el Índice Compuesto en la Consola
                    Toast.makeText(this, "Actualizando lista...", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                alertList.clear()

                if (snapshots != null) {
                    for (doc in snapshots) {
                        // Formatear la fecha
                        val timestamp = doc.getTimestamp("timestamp")
                        val dateString = if (timestamp != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            sdf.format(timestamp.toDate())
                        } else {
                            "Fecha desconocida"
                        }

                        val alert = AlertItem(
                            id = doc.id,
                            category = doc.getString("category") ?: "Alerta",
                            description = doc.getString("description") ?: "Sin detalles.",
                            status = doc.getString("status") ?: "active",
                            date = dateString,
                            userName = doc.getString("name") ?: "Yo" // Usamos "name" como en tu DB
                        )
                        alertList.add(alert)
                    }
                }

                // Asignar al adaptador
                val adapter = AlertAdapter(alertList) { alertaTocada ->
                    mostrarDetallesAlerta(alertaTocada.category, alertaTocada.description, alertaTocada.userName)
                }
                rvAlerts.adapter = adapter
            }
    }

    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?, userName: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)

        val tvCategory = view.findViewById<TextView>(R.id.tvSheetCategory)
        val tvReporter = view.findViewById<TextView>(R.id.tvSheetReporter)
        val tvDescription = view.findViewById<TextView>(R.id.tvSheetDescription)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrarSheet)

        tvCategory.text = categoria
        tvReporter.text = if (userName.isNullOrEmpty() || userName == "Yo") "Tú (Yo)" else userName
        tvDescription.text = if (descripcion.isNullOrEmpty()) "Sin detalles adicionales." else descripcion

        btnCerrar.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    // Modelo de datos para las alertas
    data class AlertItem(
        val id: String = "",
        val category: String = "",
        val description: String = "",
        val status: String = "",
        val date: String = "",
        val userName: String = ""
    )
}