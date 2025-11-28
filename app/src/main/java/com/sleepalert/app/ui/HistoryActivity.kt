package com.sleepalert.app.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.sleepalert.app.R

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Nút back
        findViewById<ImageView>(R.id.btnBackHistory).setOnClickListener {
            finish()
        }

        // Hiện tại UI là static fake data nên không cần xử lý gì thêm
        // Sau này có list thực tế thì mình refactor lại.
    }
}
