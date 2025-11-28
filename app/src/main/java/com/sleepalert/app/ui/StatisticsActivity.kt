package com.sleepalert.app.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sleepalert.app.R
import com.sleepalert.app.model.AlertRecord
import com.sleepalert.app.model.DrowsinessLevel
import java.util.Calendar
import java.util.Date

class StatisticsActivity : AppCompatActivity() {

    private lateinit var tvTotalAlerts: TextView
    private lateinit var tvTodayAlerts: TextView
    private lateinit var tvLongestAlert: TextView

    private lateinit var pbHigh: ProgressBar
    private lateinit var pbMedium: ProgressBar
    private lateinit var pbLow: ProgressBar

    private lateinit var tvHighCount: TextView
    private lateinit var tvMediumCount: TextView
    private lateinit var tvLowCount: TextView
    private lateinit var tvTips: TextView

    private val allAlerts = mutableListOf<AlertRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Nút back
        findViewById<ImageView>(R.id.btnBackStats).setOnClickListener {
            finish()
        }

        // Ánh xạ view
        tvTotalAlerts = findViewById(R.id.tvTotalAlerts)
        tvTodayAlerts = findViewById(R.id.tvTodayAlerts)
        tvLongestAlert = findViewById(R.id.tvLongestAlert)

        pbHigh = findViewById(R.id.pbHigh)
        pbMedium = findViewById(R.id.pbMedium)
        pbLow = findViewById(R.id.pbLow)

        tvHighCount = findViewById(R.id.tvHighCount)
        tvMediumCount = findViewById(R.id.tvMediumCount)
        tvLowCount = findViewById(R.id.tvLowCount)
        tvTips = findViewById(R.id.tvTips)

        // Fake nhiều dữ liệu
        generateFakeData()

        // Tính toán & đổ lên UI
        bindStatistics()
    }

    private fun generateFakeData() {
        allAlerts.clear()
        val cal = Calendar.getInstance()

        // Tạo khoảng 25–30 alert trong 7 ngày
        var idCounter = 1
        repeat(10) {
            cal.time = Date()
            cal.add(Calendar.HOUR_OF_DAY, -it * 2)
            allAlerts += AlertRecord(
                id = idCounter++,
                timestamp = cal.time,
                level = DrowsinessLevel.HIGH,
                durationSeconds = (10..30).random(),
                imagePath = null
            )
        }

        repeat(8) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -((1..3).random()))
            allAlerts += AlertRecord(
                id = idCounter++,
                timestamp = cal.time,
                level = DrowsinessLevel.MEDIUM,
                durationSeconds = (6..18).random(),
                imagePath = null
            )
        }

        repeat(7) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -((2..6).random()))
            allAlerts += AlertRecord(
                id = idCounter++,
                timestamp = cal.time,
                level = DrowsinessLevel.LOW,
                durationSeconds = (4..12).random(),
                imagePath = null
            )
        }
    }

    private fun bindStatistics() {
        if (allAlerts.isEmpty()) return

        val total = allAlerts.size
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayCount = allAlerts.count { it.timestamp.time >= todayStart }
        val longest = allAlerts.maxOf { it.durationSeconds }

        val highCount = allAlerts.count { it.level == DrowsinessLevel.HIGH }
        val mediumCount = allAlerts.count { it.level == DrowsinessLevel.MEDIUM }
        val lowCount = allAlerts.count { it.level == DrowsinessLevel.LOW }

        tvTotalAlerts.text = "Tổng cảnh báo: $total"
        tvTodayAlerts.text = "Hôm nay: $todayCount"
        tvLongestAlert.text = "Cảnh báo dài nhất: $longest giây"

        tvHighCount.text = highCount.toString()
        tvMediumCount.text = mediumCount.toString()
        tvLowCount.text = lowCount.toString()

        if (total > 0) {
            pbHigh.progress = (highCount * 100f / total).toInt()
            pbMedium.progress = (mediumCount * 100f / total).toInt()
            pbLow.progress = (lowCount * 100f / total).toInt()
        }

        tvTips.text = when {
            highCount >= mediumCount && highCount >= lowCount ->
                "Mức cảnh báo CAO xuất hiện khá nhiều. Hạn chế lái xe khi mệt, ngủ đủ giấc trước khi đi đường dài."
            mediumCount >= lowCount ->
                "Cảnh báo TRUNG BÌNH chiếm đa số. Hãy tăng thời gian nghỉ giữa các chặng lái."
            else ->
                "Bạn lái khá an toàn, chủ yếu là cảnh báo THẤP. Hãy tiếp tục duy trì thói quen tốt!"
        }
    }
}
