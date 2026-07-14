package com.example.voicemate.helpers

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

object MLKitHelper {

    /**
     * ইমেজ থেকে টেক্সট খুঁজে বের করে (OCR)
     */
    fun recognizeText(context: Context, imageUri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    if (resultText.isNotEmpty()) {
                        onSuccess(resultText)
                    } else {
                        onSuccess("ছবিতে কোনো লেখা খুঁজে পাওয়া যায়নি।")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * ইমেজ থেকে বস্তু (Object) শনাক্ত করে
     */
    fun detectObjects(context: Context, imageUri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
            val objectDetector = ObjectDetection.getClient(options)

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    if (detectedObjects.isNotEmpty()) {
                        val labels = detectedObjects.mapNotNull { obj ->
                            obj.labels.firstOrNull()?.text
                        }
                        if (labels.isNotEmpty()) {
                            onSuccess("ছবিতে দেখা যাচ্ছে: " + labels.joinToString(", "))
                        } else {
                            onSuccess("ছবিতে থাকা বস্তুগুলো শনাক্ত করা সম্ভব হয়নি।")
                        }
                    } else {
                        onSuccess("ছবিতে কোনো নির্দিষ্ট বস্তু খুঁজে পাওয়া যায়নি।")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
