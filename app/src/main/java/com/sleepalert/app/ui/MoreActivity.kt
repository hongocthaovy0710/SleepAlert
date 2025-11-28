package com.sleepalert.app.ui

import android.content.Intent
import android.os.Bundle
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

        // Lấy username từ SharedPreferences
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"
        tvUsername.text = username

        // 4 menu chính
        val btnHistory = findViewById<LinearLayout>(R.id.btnHistory)
        val btnStatistics = findViewById<LinearLayout>(R.id.btnStatistics)
        val btnProfile = findViewById<LinearLayout>(R.id.btnProfile)
        val btnUpgrade = findViewById<LinearLayout>(R.id.btnUpgrade)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        // ============================
        // MỞ LỊCH SỬ CẢNH BÁO
        // ============================
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }


        // ============================
        // MỞ THỐNG KÊ
        // ============================
        btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // ============================
        // MỞ THÔNG TIN CÁ NHÂN
        // ============================
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // ============================
        // MỞ NÂNG CẤP TÀI KHOẢN
        // ============================
        btnUpgrade.setOnClickListener {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }

        // ============================
        // ĐĂNG XUẤT
        // ============================
        btnLogout.setOnClickListener {
            val i = Intent(this, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }

        // ============================
        // BOTTOM NAV
        // ============================
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
