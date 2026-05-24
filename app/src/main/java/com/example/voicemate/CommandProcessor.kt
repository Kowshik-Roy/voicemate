package com.example.voicemate

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.example.voicemate.camera.CameraActivity
import com.example.voicemate.helpers.AppHelper
import com.example.voicemate.helpers.AppPreferenceHelper
import com.example.voicemate.helpers.DateHelper
import com.example.voicemate.helpers.SearchHelper
import com.example.voicemate.service.VoiceAccessibilityService

object CommandProcessor {

    fun processCommand(context: Context, command: String): String {
        val cmd = command.lowercase().trim()
        
        if (isGreeting(cmd)) {
            return "হ্যালো, আমি আপনার কথা শুনছি।"
        }

        // টাইপিং মোড চেক
        if (cmd.contains("টাইপিং") || cmd.contains("typing") || cmd.contains("লিখতে চাই") || cmd.contains("লিখো")) {
            return if (VoiceAccessibilityService.instance == null) {
                "টাইপিং মোড ব্যবহারের জন্য অ্যাক্সেসিবিলিটি সার্ভিস অন করতে হবে। অ্যাপের হোম পেজ থেকে পারমিশন দিন।"
            } else {
                "টাইপিং মোড চালু করা হয়েছে। আপনি যা বলবেন তা টাইপ করা হবে।"
            }
        }

        // ক্যামেরা বা অ্যাপ বন্ধ করা
        if (cmd.contains("বন্ধ") || cmd.contains("কাটো") || cmd.contains("close") || 
            cmd.contains("exit") || cmd.contains("ব্যাক") || cmd.contains("পিছনে")) {
            CameraActivity.instance?.let {
                it.finish()
                return "ক্যামেরা বন্ধ করা হচ্ছে"
            }
            return AppHelper.closeCurrentApp(context)
        }

        // তারিখ ও ভলিউম
        if ("তারিখ" in cmd || "date" in cmd) return DateHelper.getCurrentDateBengali()
        
        if ("ভলিউম" in cmd || "volume" in cmd || "সাউন্ড" in cmd) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return if ("বাড়াও" in cmd || "up" in cmd) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "ভলিউম বাড়ানো হয়েছে।"
            } else {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "ভলিউম কমানো হয়েছে।"
            }
        }

        // গুগল ও ইউটিউব সার্চ
        if (("গুগল" in cmd || "google" in cmd) && ("সার্চ" in cmd || "search" in cmd)) {
            val query = extractSearchQuery(cmd, listOf("গুগল", "search", "সার্চ"))
            return if (query.isNotEmpty()) { SearchHelper.searchInGoogle(context, query); "গুগল সার্চ করা হচ্ছে" } else "গুগলে কি সার্চ করতে হবে?"
        }

        // --- ডাইনামিক অ্যাপ ওপেনার (Dynamic App Opener) ---
        if (cmd.contains("open") || cmd.contains("খুলো") || cmd.contains("চালু কর")) {
            val appNameFromVoice = cmd.replace("open", "").replace("খুলো", "").replace("চালু কর", "").trim()
            
            if (appNameFromVoice.isNotEmpty()) {
                val installedApps = AppHelper.getAllInstalledApps(context)
                // আপনার বলা নামের সাথে ফোনের অ্যাপের নাম ম্যাচ করা হচ্ছে
                val matchedApp = installedApps.find { 
                    it.name.lowercase().contains(appNameFromVoice) || 
                    appNameFromVoice.contains(it.name.lowercase()) 
                }

                if (matchedApp != null) {
                    // চেক করা হচ্ছে আপনি সেটিংসে পারমিশন দিয়েছেন কি না
                    if (AppPreferenceHelper.isAppAllowed(context, matchedApp.packageName)) {
                        return AppHelper.openApp(context, listOf(matchedApp.packageName))
                    } else {
                        return "${matchedApp.name} ওপেন করার অনুমতি নেই। সেটিংস থেকে পারমিশন দিন।"
                    }
                }
            }
        }

        return "দুঃখিত, আমি এটি বুঝতে পারছি না।"
    }

    private fun extractSearchQuery(command: String, keywords: List<String>): String {
        var query = command
        for (word in keywords) query = query.replace(word, "")
        return query.trim()
    }

    fun isACommand(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val keywords = listOf("বন্ধ", "কাটো", "ব্যাক", "খুলো", "ওপেন", "open", "গুগল", "ইউটিউব", "চার্জ", "ভলিউম", "টাইপিং", "তারিখ")
        return keywords.any { it in cmd }
    }

    fun isGreeting(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val greetings = listOf("hello", "hi", "হ্যালো", "হাই")
        return greetings.any { it in cmd }
    }
}
