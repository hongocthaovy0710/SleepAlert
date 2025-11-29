package com.sleepalert.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.sleepalert.app.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtUser = findViewById<EditText>(R.id.username)
        val edtPass = findViewById<EditText>(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)
        val btnForgot = findViewById<TextView>(R.id.btnForgot)

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnForgot.setOnClickListener {
            Toast.makeText(
                this,
                "Chức năng quên mật khẩu đang được phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnLogin.setOnClickListener {
            val user = edtUser.text.toString().trim()
            val pass = edtPass.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = postLogin(user, pass)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, result.second, Toast.LENGTH_SHORT).show()
                    if (result.first) {

                        // 🔹 LƯU USERNAME ĐỂ MÀN MORE / PROFILE DÙNG
                        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
                        sharedPref.edit()
                            .putString("username", user)
                            .apply()

                        // Nếu vẫn muốn truyền kèm qua Home thì giữ lại cũng được
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        intent.putExtra("USERNAME", user)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun postLogin(username: String, password: String): Pair<Boolean, String> {
        return try {
            Log.d("LoginActivity", "Sending login request...")
            val url = URL("http://192.168.1.105:8080/login")   // IP server của em
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            val json = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            conn.outputStream.use {
                it.write(json.toString().toByteArray())
            }

            val code = conn.responseCode
            Log.d("LoginActivity", "Response code: $code")

            val stream = if (code in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream ?: conn.inputStream
            }

            val responseText = stream.bufferedReader().readText()
            Log.d("LoginActivity", "Response: $responseText")

            val data = JSONObject(responseText)
            val status = data.optString("status")
            val message = data.optString("message", "Lỗi server ($code)")

            Pair(status == "success", message)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Login error: ${e.message}", e)
            Pair(false, "Lỗi kết nối: ${e.message}")
        }
    }
}
