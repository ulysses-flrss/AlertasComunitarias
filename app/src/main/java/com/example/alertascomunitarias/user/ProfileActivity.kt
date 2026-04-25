package com.example.alertascomunitarias.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.user.EditProfileActivity
import com.example.alertascomunitarias.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder // 🔥 IMPORTACIÓN DEL DIÁLOGO
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvName = findViewById(R.id.tvDisplayProfileName)
        tvPhone = findViewById(R.id.tvDisplayProfilePhone)
        tvNotifs = findViewById(R.id.tvDisplayNotifications)
        tvRadius = findViewById(R.id.tvDisplayRadius)

        rvAlerts = findViewById(R.id.rvMyAlerts)
        layoutEmptyState = findViewById(R.id.layoutEmptyStateProfile)

        rvAlerts.layoutManager = LinearLayoutManager(this)
        alertList = mutableListOf()

        setupFilter()
        setupButtons()
        setupBottomNavigation()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnGoToEdit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // 🔥 NUEVA LÓGICA: Confirmación antes de cerrar sesión
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas salir de tu cuenta?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    // Si el usuario confirma, ejecutamos el código original
                    auth.signOut()
                    val intent = Intent(this, com.example.alertascomunitarias.auth.LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    // Si cancela, simplemente cerramos el cuadro de diálogo
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupFilter() {
        val categorias = arrayOf("Todas", "Robo / Asalto", "Incendio", "Accidente de Tránsito", "Actividad Sospechosa", "Emergencia Médica", "Mascota Perdida")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterProfile)
        dropdown.setAdapter(adapter)

        val switchHistorial = findViewById<SwitchMaterial>(R.id.switchHistorial)

        dropdown.setOnItemClickListener { _, _, position, _ ->
            loadUserAlerts(categorias[position], switchHistorial.isChecked)
        }

        switchHistorial.setOnCheckedChangeListener { _, isChecked ->
            val currentFilter = dropdown.text.toString().ifEmpty { "Todas" }
            loadUserAlerts(currentFilter, isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvName.text = currentUser.displayName ?: "Usuario"
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    tvPhone.text = doc.getString("phone") ?: "No registrado"
                    val notifs = doc.getBoolean("notificationsEnabled") ?: false
                    tvNotifs.text = if (notifs) "Notificaciones: Activadas" else "Notificaciones: Desactivadas"
                    tvRadius.text = "Radio de alerta: ${doc.getLong("alertRadiusKm") ?: 5} km"
                }
            }
        }
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownFilterProfile)
        val switchH = findViewById<SwitchMaterial>(R.id.switchHistorial)
        loadUserAlerts(dropdown.text.toString().ifEmpty { "Todas" }, switchH.isChecked)
    }

    private fun loadUserAlerts(categoriaFiltro: String = "Todas", mostrarResueltas: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return
        var query: Query = db.collection("alerts").whereEqualTo("userId", uid)

        if (!mostrarResueltas) query = query.whereEqualTo("status", "active")
        if (categoriaFiltro != "Todas") query = query.whereEqualTo("category", categoriaFiltro)

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                // CAZADOR DE ERRORES DE FIREBASE
                if (e != null) {
                    Log.e("FirebaseError", "¡Error en la consulta!: ", e)
                    Toast.makeText(this, "Falta índice en Firebase. Revisa el Logcat.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                alertList.clear()

                snapshots?.let {
                    for (doc in it) {
                        val timestamp = doc.getTimestamp("timestamp")
                        val dateString = if (timestamp != null) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp.toDate()) else "Sin fecha"

                        val alert = AlertItem(
                            id = doc.id,
                            category = doc.getString("category") ?: "Alerta",
                            description = doc.getString("description") ?: "",
                            status = doc.getString("status") ?: "active",
                            date = dateString,
                            userName = doc.getString("name") ?: doc.getString("userName") ?: doc.getString("usuario") ?: "Yo",
                            userId = doc.getString("userId") ?: "",
                            distance = "" // En el perfil no hace falta mostrar la distancia
                        )
                        alertList.add(alert)
                    }
                }

                // LÓGICA DEL ESTADO VACÍO
                if (alertList.isEmpty()) {
                    rvAlerts.visibility = View.GONE
                    layoutEmptyState.visibility = View.VISIBLE

                    val tvEmpty = findViewById<TextView>(R.id.tvEmptyStateProfileText)
                    if (mostrarResueltas) {
                        tvEmpty.text = "No tienes historial para esta categoría"
                    } else {
                        tvEmpty.text = "No tienes alertas activas en esta categoría"
                    }

                } else {
                    rvAlerts.visibility = View.VISIBLE
                    layoutEmptyState.visibility = View.GONE
                }

                rvAlerts.adapter = AlertAdapter(
                    alertList,
                    currentUserId = uid,
                    onAlertClick = { a -> mostrarDetallesAlerta(a.category, a.description, a.userName) },
                    onResolveClick = { a -> resolverAlerta(a.id) }
                )
            }
    }

    private fun resolverAlerta(alertId: String) {
        db.collection("alerts").document(alertId).update("status", "resuelto")
            .addOnSuccessListener { Toast.makeText(this, "Alerta marcada como resuelta", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Error al resolver la alerta", Toast.LENGTH_SHORT).show() }
    }

    private fun mostrarDetallesAlerta(categoria: String?, descripcion: String?, userName: String?) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_alert, null)
        view.findViewById<TextView>(R.id.tvSheetCategory).text = categoria
        view.findViewById<TextView>(R.id.tvSheetReporter).text = if (userName.isNullOrEmpty() || userName == "Yo" || userName == "null") "Tú (Yo)" else userName
        view.findViewById<TextView>(R.id.tvSheetDescription).text = descripcion ?: "Sin detalles."
        view.findViewById<Button>(R.id.btnCerrarSheet).setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_map -> { startActivity(Intent(this, MapHomeActivity::class.java)); false }
                R.id.nav_feed -> { startActivity(Intent(this, AlertFeedActivity::class.java)); false }
                else -> false
            }
        }
    }

    data class AlertItem(
        val id: String = "",
        val category: String = "",
        val description: String = "",
        val status: String = "",
        val date: String = "",
        val userName: String = "",
        val userId: String = "",
        val distance: String = ""
    )
}