package com.example.voicemate

import android.app.Application
import com.google.android.material.color.DynamicColors

class VoiceMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Dynamic Color (Material 3) for Android 12+
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
