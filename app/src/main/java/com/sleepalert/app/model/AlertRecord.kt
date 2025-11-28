package com.sleepalert.app.model

import java.util.Date

enum class DrowsinessLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class AlertRecord(
    val id: Int,
    val timestamp: Date,
    val level: DrowsinessLevel,
    val durationSeconds: Int,
    val imagePath: String? = null   // sau này có ảnh thì nhét path vô
)
