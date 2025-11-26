package com.sleepalert.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.sleepalert.app.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import android.graphics.Bitmap
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnTest: MaterialButton

    private lateinit var interpreter: Interpreter
    private lateinit var analyzer: DrowsinessAnalyzer
    private lateinit var cameraExecutor: ExecutorService

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewFinder = findViewById(R.id.viewFinder)
        tvStatus = findViewById(R.id.tvDetectionStatus)
        btnStart = findViewById(R.id.btnStartMonitoring)
        btnTest = findViewById(R.id.btnTestModel)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load TFLite model
        try {
            Log.i("Debug", "onCreate: ")
            interpreter = Interpreter(FileUtil.loadMappedFile(this, "best_float16.tflite"))
            analyzer = DrowsinessAnalyzer(this, interpreter, tvStatus)
        } catch (e: Exception) {
            tvStatus.text = "❌ Lỗi load model: ${e.message}"
        }

        btnStart.setOnClickListener {
            if (allPermissionsGranted()) {
                // bind camera sau khi PreviewView đã layout xong
                viewFinder.post {
                    if (!cameraBound) startCamera()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnTest.setOnClickListener { testModel() }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, analysis)
                tvStatus.text = "Camera đã bật – đang phân tích..."
                cameraBound = true
            } catch (e: Exception) {
                tvStatus.text = "❌ Lỗi camera: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun testModel() {
        try {
            val dummyBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
            val tfImage = TensorImage(DataType.FLOAT32)
            tfImage.load(dummyBitmap)

            val processor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            val processed = processor.process(tfImage)
            val out = Array(1) { FloatArray(2) }
            interpreter.run(processed.buffer, out)

            val idx = out[0].indices.maxByOrNull { out[0][it] } ?: -1
            val score = out[0][idx]

            tvStatus.text = "✅ Mô hình OK — lớp=$idx | score=$score"

        } catch (e: Exception) {
            tvStatus.text = "❌ Lỗi mô hình: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        interpreter.close()
        cameraExecutor.shutdown()
    }
}
