package com.example.alertascomunitarias.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.alertascomunitarias.R
import com.example.alertascomunitarias.adapter.AdminUserAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.view.View
import com.example.alertascomunitarias.auth.LoginActivity

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userList: MutableList<UserItem>
    private lateinit var adapter: AdminUserAdapter
    private val db = FirebaseFirestore.getInstance()

    private lateinit var spinnerRole: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_users)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.GestionUsuarios)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        userList = mutableListOf()
        adapter = AdminUserAdapter(
            userList,
            onEditClick = { user ->
                val intent = Intent(this, EditUserActivity::class.java)
                intent.putExtra("id", user.id)
                intent.putExtra("name", user.name)
                intent.putExtra("email", user.email)
                intent.putExtra("phone", user.phone)
                intent.putExtra("role", user.role)
                startActivity(intent)
            },
            onDeleteClick = { user ->
                showDeleteDialog(user)
            }
        )
        recyclerView.adapter = adapter

        // Spinner
        spinnerRole = findViewById(R.id.spinnerRole)

        val roles = resources.getStringArray(R.array.roles_array)
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerRole.adapter = adapterSpinner

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadUsers()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddUser)

        fab.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            startActivity(intent)
        }

        loadUsers()

        //  MENÚ (NO SE TOCA)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.mapView

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard ->  {
                    val intent = Intent(this, AdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_GestionAlertas -> {
                    val intent = Intent(this, AdminAlertsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_GestionUsuarios -> true
                R.id.menu_cerrar_sesion -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUsers() {
        val selectedRole = spinnerRole.selectedItem.toString()

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                userList.clear()

                for (document in result) {

                    val user = UserItem(
                        id = document.id,
                        name = document.getString("name") ?: "Sin nombre",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: "",
                        role = document.getString("role") ?: "ciudadano"
                    )

                    if (selectedRole == "Todos" || user.role == selectedRole) {
                        userList.add(user)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }
    private fun showDeleteDialog(user: UserItem) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Eliminar usuario")
        builder.setMessage("¿Seguro que deseas eliminar a ${user.name}?")

        builder.setPositiveButton("Sí") { _, _ ->
            deleteUser(user)
        }

        builder.setNegativeButton("Cancelar", null)

        builder.show()
    }
    private fun deleteUser(user: UserItem) {
        db.collection("users")
            .document(user.id)
            .delete()
            .addOnSuccessListener {
                loadUsers() // recargar lista
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Error al eliminar", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }
}