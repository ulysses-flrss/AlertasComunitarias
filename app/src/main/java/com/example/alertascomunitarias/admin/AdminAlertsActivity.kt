package com.example.alertascomunitarias.admin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alertascomunitarias.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminAlertsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var alertList: MutableList<AdminAlertItem>
    private lateinit var adapter: AdminAlertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_alerts)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rvAdminAlerts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        alertList = mutableListOf()

        loadAlerts()
    }

    private fun loadAlerts() {

        db.collection("alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.e("AdminAlerts", "Error", e)
                    Toast.makeText(this, "Error cargando alertas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                alertList.clear()

                snapshots?.forEach { doc ->
                    val alert = AdminAlertItem(
                        id = doc.id,
                        category = doc.getString("category") ?: "Sin categoría",
                        description = doc.getString("description") ?: "",
                        userName = doc.getString("userName") ?: "Anónimo",
                        status = doc.getString("status") ?: "active"
                    )

                    alertList.add(alert)
                }

                if (!::adapter.isInitialized) {
                    adapter = AdminAlertAdapter(alertList)
                    recyclerView.adapter = adapter
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
    }
}