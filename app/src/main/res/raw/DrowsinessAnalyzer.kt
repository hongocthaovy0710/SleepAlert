package raw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaPlayer
import android.util.Log
import android.widget.TextView
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

    // ====== LOGIC BUỒN NGỦ ======
    private var sleepyStartTime: Long = 0L           // thời điểm bắt đầu nhắm mắt
    private val sleepyDuration = 2_000L              // ms (2s) - em muốn 3s thì đổi thành 3_000L

    private val inputBuffer = ByteBuffer.allocateDirect(
        WIDTH * HEIGHT * PIXEL_SIZE * NUM_BYTES_PER_CHANNEL
    ).apply { order(ByteOrder.nativeOrder()) }

    // ====== ÂM THANH CẢNH BÁO ======
    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(context, R.raw.alert_beep).apply {
            isLooping = false
        }
    }

    private var lastPlayTime = 0L
    private val MIN_INTERVAL_MS = 5_000L // 5s mới cho phép kêu lại 1 lần

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toRgbBitmap() ?: return
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true)
            convertBitmapToByteBuffer(resizedBitmap)
            interpreter.run(inputBuffer, output)

            // Debug nhẹ output (em muốn thì giữ, không thì xoá)
            /*
            for (i in 0 until 10) {
                val arr = output[0][i]
                Log.d("YOLO_OUT", "arr[$i][0..10]=${arr.take(10)}, last2=${arr.takeLast(2)}")
            }
            */

            val (predClass, predScore) = decodeYoloOutput(output)

            val currentTime = System.currentTimeMillis()
            val isSleepy = (predClass == 0)

            if (isSleepy) {
                if (sleepyStartTime == 0L) {
                    sleepyStartTime = currentTime
                }

                val duration = currentTime - sleepyStartTime

                if (duration >= sleepyDuration) {
                    // ✅ BUỒN NGỦ ĐỦ LÂU → CẢNH BÁO + KÊU ÂM THANH
                    tvStatus.post {
                        tvStatus.text = "⚠️ BUỒN NGỦ! (Score: %.2f)".format(predScore)
                    }
                    playAlertSoundIfNeeded()
                } else {
                    // Đang tích luỹ thời gian nhắm mắt
                    tvStatus.post {
                        val seconds = duration / 1000.0f
                        tvStatus.text =
                            "Mắt nhắm… %.1fs (Score: %.2f)".format(seconds, predScore)
                    }
                }
            } else {
                // 🟢 TỈNH TÁO → reset timer
                sleepyStartTime = 0L
                tvStatus.post {
                    tvStatus.text = "🟢 TỈNH TÁO (Score: %.2f)".format(predScore)
                }
            }
        } catch (e: Exception) {
            tvStatus.post { tvStatus.text = "❌ Lỗi: ${e.message}" }
            Log.e("DrowsinessAnalyzer", "Analyze error", e)
        } finally {
            imageProxy.close()
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

    // 🔊 Hàm kêu tiếng bíp (chống spam)
    private fun playAlertSoundIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastPlayTime < MIN_INTERVAL_MS) return  // chưa đủ 5s thì thôi

        lastPlayTime = now
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.seekTo(0) // đang kêu thì tua về đầu
            } else {
                mediaPlayer.start()
            }
        } catch (e: Exception) {
            Log.e("DrowsinessAnalyzer", "Error playing sound: ${e.message}")
        }
    }

    /**
     * ✅ LOGIC PHÂN LOẠI CHÍNH: Sử dụng TỔNG ĐIỂM TIN CẬY của Class 0 (Buồn ngủ).
     */
    private fun decodeYoloOutput(yoloOutput: Array<Array<FloatArray>>): Pair<Int, Float> {
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

        return if (sleepyScoreSum > sumThreshold) {
            0 to maxScore       // Class 0 (Buồn ngủ)
        } else {
            1 to maxScore       // Class 1 (Tỉnh táo)
        }
    }

    // Cho testModel() dùng
    companion object {
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

            return if (sleepyScoreSum > sumThreshold) {
                0 to maxScore
            } else {
                1 to maxScore
            }
        }
    }

    // Gọi từ HomeActivity.onDestroy()
    fun release() {
        try {
            mediaPlayer.release()
        } catch (_: Exception) { }
    }
}

// ====== EXTENSION CHUYỂN ImageProxy → Bitmap ======
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
