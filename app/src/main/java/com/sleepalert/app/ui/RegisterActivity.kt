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

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtUser = findViewById<EditText>(R.id.username)
        val edtPass = findViewById<EditText>(R.id.password)
        val edtEmail = findViewById<EditText>(R.id.email)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            val user = edtUser.text.toString()
            val pass = edtPass.text.toString()
            val email = edtEmail.text.toString()

            if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val result = postRegister(user, pass, email)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegisterActivity, result.second, Toast.LENGTH_SHORT).show()
                    if (result.first) {
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun postRegister(username: String, password: String, email: String): Pair<Boolean, String> {
        return try {
            Log.d("RegisterActivity", "Sending request to server...")
            val url = URL("http://172.16.0.227:8080/register")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val json = JSONObject()
            json.put("username", username)
            json.put("password", password)
            json.put("email", email)

            Log.d("RegisterActivity", "Request data: $json")
            conn.outputStream.write(json.toString().toByteArray())
            
            val responseCode = conn.responseCode
            Log.d("RegisterActivity", "Response code: $responseCode")
            
            val response = conn.inputStream.bufferedReader().readText()
            Log.d("RegisterActivity", "Response: $response")
            
            val data = JSONObject(response)
            Pair(data.getString("status") == "success", data.getString("message"))
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error: ${e.message}", e)
            Pair(false, "Lỗi kết nối: ${e.message}")
        }
    }
}
