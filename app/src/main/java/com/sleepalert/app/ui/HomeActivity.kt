package com.sleepalert.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sleepalert.app.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Hiển thị thông báo chào mừng
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = "Xin chào!"
        
        // Button bắt đầu giám sát
        val btnStartMonitoring = findViewById<Button>(R.id.btnStartMonitoring)
        btnStartMonitoring.setOnClickListener {
            Toast.makeText(this, "Tính năng giám sát đang được phát triển", Toast.LENGTH_SHORT).show()
        }
        
        // Button đăng xuất
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            // Quay về màn hình Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
