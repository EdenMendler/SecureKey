package com.example.securekey.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectionManager(
    private val context: Context,
    private val previewView: PreviewView?,
    private val onLockStatusChanged: (Int, Boolean) -> Unit
) {
    private val TAG = "FaceDetectionManager"
    private val CAMERA_PERMISSION_REQUEST_CODE = 102
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var isFaceDetectionActive = false
    private var consecutiveSmileDetections = 0
    private val REQUIRED_CONSECUTIVE_SMILE_DETECTIONS = 5

    init {
        initializeFaceDetection()
        requestCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initializeFaceDetection() {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(realTimeOpts)
    }

    private fun requestCameraPermission() {
        if (!allCameraPermissionsGranted()) {
            (context as? FragmentActivity)?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun allCameraPermissionsGranted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun startFaceDetection(lockStates: List<Boolean>) {
        if (!allCameraPermissionsGranted() || lockStates.size <= 5 || lockStates[5]) {
            Log.d(TAG, "Not starting face detection: permissions=${allCameraPermissionsGranted()}, lock5=${lockStates.getOrNull(5)}")
            return
        }

        isFaceDetectionActive = true
        Log.d(TAG, "Starting face detection")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, FaceAnalyzer { faces ->
                            processFaceDetection(faces)
                        })
                    }

                try {
                    cameraProvider.unbindAll()
                    try {
                        val frontCameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()

                        cameraProvider.bindToLifecycle(
                            context as FragmentActivity,
                            frontCameraSelector,
                            imageAnalysis
                        )

                        Log.d(TAG, "Front camera bound successfully")
                        previewView?.let { preview ->
                            (context as? FragmentActivity)?.runOnUiThread {
                                if (preview.visibility != android.view.View.VISIBLE) {
                                    preview.visibility = android.view.View.VISIBLE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to bind front camera: ${e.message}")

                        val backCameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        cameraProvider.bindToLifecycle(
                            context as FragmentActivity,
                            backCameraSelector,
                            imageAnalysis
                        )

                        Log.d(TAG, "Back camera bound successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind camera use cases: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get camera provider: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopFaceDetection() {
        if (!isFaceDetectionActive) return

        isFaceDetectionActive = false
        Log.d(TAG, "Stopping face detection")

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}")
        }
    }

    private fun processFaceDetection(faces: List<Face>) {
        Log.d(TAG, "Processing faces: ${faces.size}")

        if (!isFaceDetectionActive) {
            Log.d(TAG, "Face detection not active, skipping")
            return
        }

        if (faces.isNotEmpty()) {
            val face = faces[0]
            face.smilingProbability?.let { smileProb ->
                Log.d(TAG, "Smile probability: $smileProb")

                if (smileProb > 0.8) {
                    consecutiveSmileDetections++
                    Log.d(TAG, "Consecutive smile detections: $consecutiveSmileDetections/$REQUIRED_CONSECUTIVE_SMILE_DETECTIONS")

                    if (consecutiveSmileDetections >= REQUIRED_CONSECUTIVE_SMILE_DETECTIONS) {
                        Log.d(TAG, "Detected enough smiles! Unlocking...")
                        (context as? FragmentActivity)?.runOnUiThread {
                            onLockStatusChanged(5, true)
                            isFaceDetectionActive = false
                        }
                    }
                } else {
                    if (consecutiveSmileDetections > 0) {
                        Log.d(TAG, "Resetting smile counter")
                    }
                    consecutiveSmileDetections = 0
                }
            }
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted")
                } else {
                    Log.e(TAG, "Camera permission denied")
                    Toast.makeText(
                        context,
                        "Camera permission is required for smile detection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    inner class FaceAnalyzer(private val listener: (List<Face>) -> Unit) : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        listener(faces)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error detecting faces: ${e.message}")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}