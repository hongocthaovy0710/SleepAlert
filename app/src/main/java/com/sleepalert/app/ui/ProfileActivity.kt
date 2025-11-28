package com.sleepalert.app.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sleepalert.app.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Nút quay lại
        findViewById<ImageView>(R.id.btnBackProfile).setOnClickListener {
            finish()
        }

        // Lấy view
        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvEmailDisplay = findViewById<TextView>(R.id.tvEmailDisplay)

        val tvNameValue = findViewById<TextView>(R.id.tvNameValue)
        val tvPhoneValue = findViewById<TextView>(R.id.tvPhoneValue)
        val tvAddressValue = findViewById<TextView>(R.id.tvAddressValue)

        // Lấy dữ liệu user
        val sp = getSharedPreferences("user_data", MODE_PRIVATE)

        val email = sp.getString("email", "email@example.com") ?: "email@example.com"
        val name  = sp.getString("full_name", "User") ?: "User"
        val phone = sp.getString("phone", "Chưa cập nhật") ?: "Chưa cập nhật"
        val address = sp.getString("address", "Chưa cập nhật") ?: "Chưa cập nhật"

        // Đổ lên UI
        tvFullName.text = name
        tvEmailDisplay.text = email

        tvNameValue.text = name
        tvPhoneValue.text = phone
        tvAddressValue.text = address
    }
}
