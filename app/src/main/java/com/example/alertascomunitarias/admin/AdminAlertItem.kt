package com.example.alertascomunitarias.admin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.alertascomunitarias.R

data class AdminAlertItem(
    val id: String = "",
    val category: String = "",
    val description: String = "",
    val userName: String = "",
    val status: String = ""
)