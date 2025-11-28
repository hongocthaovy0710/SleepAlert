package com.sleepalert.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.sleepalert.app.R
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeActivity : AppCompatActivity() {

    // UI
    private lateinit var viewFinder: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnTest: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var bottomNav: BottomNavigationView

    // Camera + ML
    private lateinit var interpreter: Interpreter
    private var analyzer: DrowsinessAnalyzer? = null
    private lateinit var cameraExecutor: ExecutorService

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ====== LẤY USERNAME TỪ LOGIN ======
        val username = intent.getStringExtra("USERNAME") ?: "Tài xế"

        // ====== ÁNH XẠ VIEW ======
        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.text = "Xin chào, $username!"

        viewFinder = findViewById(R.id.viewFinder)
        tvStatus = findViewById(R.id.tvDetectionStatus)
        btnStart = findViewById(R.id.btnStartMonitoring)
        btnTest = findViewById(R.id.btnTestModel)
        bottomNav = findViewById(R.id.bottomNav)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ====== LOAD MODEL TFLITE ======
        try {
            Log.i("HomeActivity", "Loading TFLite model...")
            val model = FileUtil.loadMappedFile(this, "best_float32.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)

            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.d("MODEL_INFO", "Input shape: ${inputShape.contentToString()}")
            Log.d("MODEL_INFO", "Output shape: ${outputShape.contentToString()}")

            analyzer = DrowsinessAnalyzer(this, interpreter, tvStatus)
            tvStatus.text = "✅ Model loaded - Input: ${inputShape[1]}x${inputShape[2]}"
        } catch (e: Exception) {
            tvStatus.text = "❌ Lỗi load model: ${e.message}"
            Log.e("HomeActivity", "Model loading error", e)
        }

        // ====== NÚT BẮT ĐẦU GIÁM SÁT ======
        btnStart.setOnClickListener {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    if (!cameraBound) startCamera()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        // ====== NÚT TEST MODEL ======
        btnTest.setOnClickListener { testModelWithFace() }

        // ====== BOTTOM NAV ======
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_chat -> {
                    Toast.makeText(
                        this,
                        "Chat bot đang được phát triển",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

                R.id.nav_more -> {
                    val i = Intent(this, MoreActivity::class.java)
                    i.putExtra("USERNAME", username)
                    startActivity(i)
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    // ====== PERMISSION ======
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this, "Quyền camera bị từ chối", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ====== CAMERA XỬ LÝ ======
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(viewFinder.display.rotation)
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetRotation(viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer!!) }


            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, analysis)
                tvStatus.text = "📷 Camera đã bật - Đang phân tích..."
                cameraBound = true
            } catch (e: Exception) {
                tvStatus.text = "❌ Lỗi camera: ${e.message}"
                Log.e("HomeActivity", "Camera start error", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ====== TEST MODEL VỚI ẢNH GIẢ KHUÔN MẶT ======
    private fun testModelWithFace() {
        Thread {
            try {
                val inputShape = interpreter.getInputTensor(0).shape()
                val WIDTH = inputShape[1]
                val HEIGHT = inputShape[2]

                val dummyBitmap =
                    Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dummyBitmap)

                // Nền trắng
                canvas.drawColor(Color.WHITE)

                // Vẽ "khuôn mặt" giả
                val facePaint = Paint().apply {
                    color = Color.rgb(255, 220, 177)
                    style = Paint.Style.FILL
                }
                val faceRect = RectF(
                    WIDTH * 0.2f, HEIGHT * 0.2f,
                    WIDTH * 0.8f, HEIGHT * 0.8f
                )
                canvas.drawOval(faceRect, facePaint)

                // Mắt
                val eyePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(WIDTH * 0.35f, HEIGHT * 0.4f, WIDTH * 0.05f, eyePaint)
                canvas.drawCircle(WIDTH * 0.65f, HEIGHT * 0.4f, WIDTH * 0.05f, eyePaint)

                // Chuẩn bị buffer input
                val inputBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 3 * 4)
                    .apply { order(ByteOrder.nativeOrder()) }

                val intValues = IntArray(WIDTH * HEIGHT)
                dummyBitmap.getPixels(intValues, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
                var pixel = 0
                for (y in 0 until HEIGHT) {
                    for (x in 0 until WIDTH) {
                        val v = intValues[pixel++]
                        inputBuffer.putFloat(((v shr 16) and 0xFF) / 255f)
                        inputBuffer.putFloat(((v shr 8) and 0xFF) / 255f)
                        inputBuffer.putFloat((v and 0xFF) / 255f)
                    }
                }

                val output = Array(1) { Array(10) { FloatArray(8400) } }
                interpreter.run(inputBuffer, output)

                runOnUiThread {
                    tvStatus.text =
                        "🧪 Đã test với ảnh giả khuôn mặt – xem logcat để debug"
                }

                Log.d("TEST_DEBUG", "=== TEST OUTPUT VALUES ===")
                for (anchorIdx in 0 until minOf(10, 8400)) {
                    val values = FloatArray(10) { i -> output[0][i][anchorIdx] }
                    val maxVal = values.maxOrNull() ?: 0f
                    if (maxVal > 0.01f) {
                        Log.d(
                            "TEST_DEBUG",
                            "Anchor $anchorIdx: ${values.take(7).joinToString()}"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("TestModel", "Error testing model", e)
                runOnUiThread {
                    tvStatus.text = "❌ Lỗi test: ${e.message}"
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraProvider?.unbindAll()
            if (::interpreter.isInitialized) {
                interpreter.close()
            }
            analyzer?.release()   // giải phóng MediaPlayer nếu có
        } catch (_: Exception) {
        }
        cameraExecutor.shutdown()
    }

}
