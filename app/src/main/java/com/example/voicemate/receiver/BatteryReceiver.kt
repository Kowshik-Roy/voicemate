package com.example.voicemate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.voicemate.service.BackgroundVoiceService

class BatteryReceiver : BroadcastReceiver() {

    private var lastAnnouncedLevel = -1

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level * 100) / scale


            if (batteryPct % 10 == 0 && batteryPct != lastAnnouncedLevel) {
                lastAnnouncedLevel = batteryPct
                BackgroundVoiceService.instance?.speak("আপনার ফোনের বর্তমান চার্জ $batteryPct শতাংশ।")
            }


            if (batteryPct < 20 && batteryPct % 5 == 0 && batteryPct != lastAnnouncedLevel) {
                lastAnnouncedLevel = batteryPct
                BackgroundVoiceService.instance?.speak("সতর্কতা! আপনার ফোনের চার্জ খুব কম। দয়া করে চার্জে দিন।")
            }
        }
    }
}
