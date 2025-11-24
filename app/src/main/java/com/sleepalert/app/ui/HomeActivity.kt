package com.sleepalert.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sleepalert.app.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Lấy username từ Login
        val username = intent.getStringExtra("USERNAME") ?: "Tài xế"

        val tvWelcome = findViewById<TextView>(R.id.tvTitle)
        tvWelcome.text = "Xin chào, $username!"

        val btnStartMonitoring = findViewById<Button>(R.id.btnStartMonitoring)
        btnStartMonitoring.setOnClickListener {
            Toast.makeText(this, "Tính năng giám sát đang được phát triển", Toast.LENGTH_SHORT).show()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_chat -> {
                    Toast.makeText(this, "Chat bot đang được phát triển", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_more -> {
                    val intent = Intent(this, MoreActivity::class.java)
                    intent.putExtra("USERNAME", username)   // chuyển tiếp sang More
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }
}

