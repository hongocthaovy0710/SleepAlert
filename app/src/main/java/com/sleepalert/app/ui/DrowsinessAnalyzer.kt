package com.sleepalert.app.ui

import android.content.Context
import android.graphics.*
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.sleepalert.app.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.maxBy

class DrowsinessAnalyzer(
    private val context: Context,
    private val interpreter: Interpreter,
    private val tvStatus: TextView
) : ImageAnalysis.Analyzer {

    // Kích thước đầu vào mô hình được suy ra là 640x640 (tương ứng 4,915,200 bytes FLOAT32)
    private val WIDTH = 640
    private val HEIGHT = 640
    private val PIXEL_SIZE = 3 // Kênh RGB

    // THAY ĐỔI QUAN TRỌNG: Kích thước byte cho Float32 (4) hoặc Uint8 (1)
    private val NUM_BYTES_PER_CHANNEL = 4 // Giả định là Float32, 4 bytes/float

    private val output = Array(1)  { Array(10) {FloatArray(8400) }} // 0: Drowsy, 1: Awake

    // Tạo ByteBuffer cho đầu vào, kích thước phải KHỚP CHÍNH XÁC với mô hình
    private val inputBuffer = ByteBuffer.allocateDirect(
        WIDTH * HEIGHT * PIXEL_SIZE * NUM_BYTES_PER_CHANNEL
    ).apply {
        order(ByteOrder.nativeOrder())
    }

    // Không cần ImageProcessor vì chúng ta tự xử lý

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toRgbBitmap() ?: return

            // 1. Resize Bitmap thủ công (BẮT BUỘC)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true)

            // 2. Chuyển Bitmap thành ByteBuffer (Float32)
            convertBitmapToByteBuffer(resizedBitmap)

            // Run model
            interpreter.run(inputBuffer, output)

            val probs = output[0].map { arr -> arr.average().toFloat() }
            val idx = probs.indices.maxBy { probs[it] }

            val score = probs[idx]

            val text = if (idx == 0)
                "⚠️ BUỒN NGỦ! Score: ${"%.2f".format(idx)}"
            else
                "🟢 TỈNH TÁO — Score: ${"%.2f".format(idx)}"

            tvStatus.post {
                tvStatus.text = text
            }

        } catch (e: Exception) {
            tvStatus.post {
                tvStatus.text = "❌ Lỗi: ${e.message}"
            }
        } finally {
            imageProxy.close()
        }
    }

    // Hàm chuyển Bitmap sang ByteBuffer (Float32, chuẩn hóa về 0-1)
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        inputBuffer.rewind() // Reset buffer
        val intValues = IntArray(WIDTH * HEIGHT)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until WIDTH) {
            for (j in 0 until HEIGHT) {
                val `val` = intValues[pixel++]

                // Chuẩn hóa Float32: pixel [0, 255] sang [0, 1]
                inputBuffer.putFloat(((`val` shr 16 and 0xFF).toFloat() / 255.0f)) // Red
                inputBuffer.putFloat(((`val` shr 8 and 0xFF).toFloat() / 255.0f))  // Green
                inputBuffer.putFloat(((`val` and 0xFF).toFloat() / 255.0f))       // Blue
            }
        }
    }
}

/**
 * Chuyển ImageProxy (YUV_420_888) sang Bitmap ARGB_8888
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun ImageProxy.toRgbBitmap(): Bitmap? {
    val image = this.image ?: return null

    // Lỗi có thể xảy ra ở đây do cách copy byte thủ công, nhưng chúng ta sẽ dùng
    // cách an toàn (Jpeg nén) để chuyển YUV sang Bitmap.
    val planes = image.planes
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // Tạo mảng NV21
    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y (Luminance)
    yBuffer.get(nv21, 0, ySize)

    // Copy UV (Chrominance)
    // Cần phải xử lý pixel stride và row stride để copy đúng thứ tự V và U
    val vIndex = ySize
    val uIndex = ySize + vBuffer.remaining()

    // Đây là phần phức tạp, nhưng thay vì copy phức tạp, chúng ta sẽ dùng Jpeg nén.
    // Dữ liệu trong ImageProxy là YUV_420_888. Ta phải chuyển nó về NV21 để YuvImage hoạt động đúng.
    // Lần này ta sẽ dùng thư viện tiện ích để chuyển đổi YUV -> NV21 nếu có thể.
    // Giữ nguyên logic nén Jpeg vì nó là cách đáng tin cậy nhất để chuyển đổi định dạng
    // mà không bị lỗi stride.

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()

    // Nén JPEG để chuyển đổi định dạng YUV -> RGB
    yuvImage.compressToJpeg(Rect(0, 0, this.width, this.height), 90, out)
    val byteArray = out.toByteArray()

    // Decode JPEG sang ARGB_8888
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
}