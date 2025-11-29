package com.sleepalert.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaPlayer
import android.util.Log
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sleepalert.app.R
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DrowsinessAnalyzer(
    private val context: Context,
    private val interpreter: Interpreter,
    private val tvStatus: TextView
) : ImageAnalysis.Analyzer {

    private val WIDTH = 640
    private val HEIGHT = 640
    private val PIXEL_SIZE = 3
    private val NUM_BYTES_PER_CHANNEL = 4 // Float32

    private val output = Array(1) { Array(10) { FloatArray(8400) } }
    private var sleepyStartTime: Long = 0 // thời điểm bắt đầu nhắm mắt
    private val sleepyDuration = 2000L    // 2 giây

    // THÊM ÂM THANH
    private var mediaPlayer: MediaPlayer? = null
    private var isSleepyState = false // Theo dõi trạng thái buồn ngủ

    private val inputBuffer = ByteBuffer.allocateDirect(
        WIDTH * HEIGHT * PIXEL_SIZE * NUM_BYTES_PER_CHANNEL
    ).apply { order(ByteOrder.nativeOrder()) }

    init {
        initializeMediaPlayer()
    }

    // THÊM HÀM KHỞI TẠO ÂM THANH
    private fun initializeMediaPlayer() {
        try {
            // Sử dụng âm thanh từ raw folder
            mediaPlayer = MediaPlayer.create(context, R.raw.alert_beep)
            mediaPlayer?.isLooping = true // Lặp lại âm thanh
            Log.d("Audio", "✅ Đã load file âm thanh")
        } catch (e: Exception) {
            Log.e("Audio", "❌ Lỗi load âm thanh: ${e.message}")
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toRgbBitmap() ?: return
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true)
            convertBitmapToByteBuffer(resizedBitmap)
            interpreter.run(inputBuffer, output)

            for (i in 0 until 10) {
                val arr = output[0][i]
                println("arr[0..10]: ${arr.take(10)}, last2: ${arr.takeLast(2)}")
            }

            val (predClass, predScore) = decodeYoloOutput(output)

            val currentTime = System.currentTimeMillis()

            // Quyết định cuối cùng dựa trên kết quả giải mã
            val isCurrentlySleepy = predClass == 0

            // ĐIỀU KHIỂN ÂM THANH THEO TRẠNG THÁI
            controlAlertSound(isCurrentlySleepy)

            if (isCurrentlySleepy) {
                if (sleepyStartTime == 0L) {
                    sleepyStartTime = currentTime
                }

                val duration = currentTime - sleepyStartTime

                if (duration >= sleepyDuration) {
                    tvStatus.post {
                        tvStatus.text = "⚠️ BUỒN NGỦ! (Score: %.2f)".format(predScore)
                    }
                } else {
                    // Hiển thị thời gian nhắm mắt đang tích lũy
                    tvStatus.post {
                        val seconds = duration / 1000.0f
                        tvStatus.text = "Mắt nhắm… %.1fs (Score: %.2f)".format(seconds, predScore)
                    }
                }
            } else {
                // ⭐ LUÔN CẬP NHẬT TRẠNG THÁI TỈNH TÁO KHI isSleepy = FALSE
                sleepyStartTime = 0L
                tvStatus.post {
                    tvStatus.text = "🟢 TỈNH TÁO (Score: %.2f)".format(predScore)
                }
            }
        } catch (e: Exception) {
            tvStatus.post { tvStatus.text = "❌ Lỗi: ${e.message}" }
        } finally {
            imageProxy.close()
        }
    }

    // HÀM MỚI: Điều khiển âm thanh theo trạng thái
    private fun controlAlertSound(isCurrentlySleepy: Boolean) {
        if (isCurrentlySleepy && !isSleepyState) {
            // Chuyển từ tỉnh táo sang buồn ngủ: BẬT âm thanh
            startAlertSound()
            isSleepyState = true
            Log.d("Alert", "🔊 BẬT âm thanh - Bắt đầu buồn ngủ")
        } else if (!isCurrentlySleepy && isSleepyState) {
            // Chuyển từ buồn ngủ sang tỉnh táo: TẮT âm thanh
            stopAlertSound()
            isSleepyState = false
            Log.d("Alert", "🔇 TẮT âm thanh - Đã tỉnh táo")
        }
        // Nếu trạng thái không đổi thì không làm gì
    }

    private fun startAlertSound() {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    Log.d("Alert", "🎵 Âm thanh bắt đầu phát")
                }
            }
        } catch (e: Exception) {
            Log.e("Alert", "❌ Lỗi phát âm thanh: ${e.message}")
        }
    }

    private fun stopAlertSound() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0) // Reset về đầu
                    Log.d("Alert", "🔇 Âm thanh đã dừng")
                }
            }
        } catch (e: Exception) {
            Log.e("Alert", "❌ Lỗi dừng âm thanh: ${e.message}")
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        val intValues = IntArray(WIDTH * HEIGHT)
        bitmap.getPixels(intValues, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        var pixel = 0
        for (i in 0 until WIDTH) {
            for (j in 0 until HEIGHT) {
                val value = intValues[pixel++]
                inputBuffer.putFloat((value shr 16 and 0xFF) / 255f)
                inputBuffer.putFloat((value shr 8 and 0xFF) / 255f)
                inputBuffer.putFloat((value and 0xFF) / 255f)
            }
        }
    }

    /**
     * ✅ LOGIC PHÂN LOẠI CHÍNH: Sử dụng TỔNG ĐIỂM TIN CẬY của Class 0 (Buồn ngủ).
     */
    private fun decodeYoloOutput(yoloOutput: Array<Array<FloatArray>>): Pair<Int, Float> {
        var maxScore = 0f
        var sleepyScoreSum = 0f

        // ⭐ ĐIỀU CHỈNH NGƯỠNG: Tăng từ 0.10f lên 0.20f (20%) để lọc chớp mắt thông thường.
        val sumThreshold = 0.0868f

        for (i in 0 until 10) {
            val arr = yoloOutput[0][i]
            val classProbs = arr.takeLast(2).toFloatArray()
            val objectness = arr[4]

            // 1. TÍNH TỔNG ĐIỂM CHO CLASS 0 (Buồn ngủ)
            sleepyScoreSum += objectness * classProbs[0]

            // 2. Vẫn tìm max score đơn lẻ (dùng để hiển thị độ tin cậy)
            for (c in classProbs.indices) {
                val score = objectness * classProbs[c]
                if (score > maxScore) {
                    maxScore = score
                }
            }
        }

        // Quyết định cuối cùng
        if (sleepyScoreSum > sumThreshold) {
            return Pair(0, maxScore) // Class 0 (Buồn ngủ)
        }

        return Pair(1, maxScore) // Class 1 (Tỉnh táo)
    }

    // THÊM HÀM DỌN DẸP
    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d("Audio", "🔇 Đã giải phóng MediaPlayer")
        } catch (e: Exception) {
            Log.e("Audio", "❌ Lỗi giải phóng MediaPlayer: ${e.message}")
        }
    }

    companion object {
        // Giữ nguyên hàm này cho mục đích testModel()
        fun decodeYoloOutputStatic(yoloOutput: Array<Array<FloatArray>>): Pair<Int, Float> {
            var maxScore = 0f
            var sleepyScoreSum = 0f
            val sumThreshold = 0.0868f

            for (i in 0 until 10) {
                val arr = yoloOutput[0][i]
                val classProbs = arr.takeLast(2).toFloatArray()
                val objectness = arr[4]

                sleepyScoreSum += objectness * classProbs[0]

                for (c in classProbs.indices) {
                    val score = objectness * classProbs[c]
                    if (score > maxScore) {
                        maxScore = score
                    }
                }
            }

            if (sleepyScoreSum > sumThreshold) {
                return Pair(0, maxScore)
            }
            return Pair(1, maxScore)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toRgbBitmap(): Bitmap? {
    val image = this.image ?: return null

    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val byteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}
