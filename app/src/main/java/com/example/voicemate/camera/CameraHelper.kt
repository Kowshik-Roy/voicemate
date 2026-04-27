package com.example.voicemate.camera

import android.content.Context
import android.content.Intent

object CameraHelper {

    fun openCamera(context: Context): String {
        return try {
            val intent = Intent(context, CameraActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening camera."
        } catch (e: Exception) {
            "Failed to open camera."
        }
    }
}
