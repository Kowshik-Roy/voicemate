package com.example.voicemate.camera

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.voicemate.databinding.ActivityCameraBinding
import com.example.voicemate.helpers.MLKitHelper
import com.example.voicemate.service.BackgroundVoiceService
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        var instance: CameraActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        instance = this
        startCamera()

        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // দৃষ্টিহীনদের সুবিধার জন্য স্ক্রিন খুললেই একটি নির্দেশনা প্রদান
        BackgroundVoiceService.instance?.speak("ক্যামেরা ওপেন হয়েছে। ছবি তুলতে স্ক্রিনের নিচের মাঝখানে ক্লিক করুন।", false)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VoiceMate")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        BackgroundVoiceService.instance?.speak("ছবি তোলা হচ্ছে, দয়া করে স্থির থাকুন।", false)

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    BackgroundVoiceService.instance?.speak("দুঃখিত, ছবি তোলা সম্ভব হয়নি।")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    processImage(savedUri)
                }
            }
        )
    }

    private fun processImage(uri: Uri) {
        BackgroundVoiceService.instance?.speak("ছবি প্রসেসিং করা হচ্ছে।", false)
        
        // প্রথমে টেক্সট শনাক্ত করার চেষ্টা করি
        MLKitHelper.recognizeText(this, uri, { text ->
            if (text.contains("খুঁজে পাওয়া যায়নি")) {
                // যদি টেক্সট না পাওয়া যায়, তবে অবজেক্ট ডিটেকশন করি
                detectObjects(uri)
            } else {
                BackgroundVoiceService.instance?.speak("ছবিতে লেখা আছে: $text")
            }
        }, {
            detectObjects(uri)
        })
    }

    private fun detectObjects(uri: Uri) {
        MLKitHelper.detectObjects(this, uri, { result ->
            BackgroundVoiceService.instance?.speak(result)
        }, {
            BackgroundVoiceService.instance?.speak("দুঃখিত, ছবিতে কি আছে তা আমি বুঝতে পারছি না।")
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        cameraExecutor.shutdown()
    }
}
