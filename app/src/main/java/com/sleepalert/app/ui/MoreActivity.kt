package com.sleepalert.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.sleepalert.app.R

class MoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_more

        // Get user data from SharedPreferences or Intent
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"
        tvUsername.text = username

        // Menu Items - 4 Mục chính
        val btnHistory = findViewById<LinearLayout>(R.id.btnHistory)
        val btnStatistics = findViewById<LinearLayout>(R.id.btnStatistics)
        val btnProfile = findViewById<LinearLayout>(R.id.btnProfile)
        val btnUpgrade = findViewById<LinearLayout>(R.id.btnUpgrade)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        // ---- BUTTON HANDLERS ----

        btnHistory.setOnClickListener {
            Toast.makeText(this, "Lịch sử cảnh báo đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnStatistics.setOnClickListener {
            Toast.makeText(this, "Thống kê cảnh báo đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Thông tin cá nhân đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnUpgrade.setOnClickListener {
            Toast.makeText(this, "Nâng cấp tài khoản đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            val i = Intent(this, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }

        // ---- BOTTOM NAV ----

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_chat -> {
                    Toast.makeText(this, "Chat đang phát triển", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_more -> true

                else -> false
            }
        }
    }
}