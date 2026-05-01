package com.example.voicemate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import android.provider.MediaStore
import com.example.voicemate.camera.CameraActivity
import com.example.voicemate.helpers.AppHelper
import com.example.voicemate.helpers.DateHelper
import com.example.voicemate.helpers.SearchHelper

object CommandProcessor {

    fun processCommand(context: Context, command: String): String {
        val cmd = command.lowercase().trim()
        
        if (cmd.contains("hello") || cmd.contains("hi") || cmd.contains("hey") || 
            cmd.contains("হ্যালো") || cmd.contains("হাই") || cmd.contains("হে")) {
            return "হ্যালো, আমি আপনার কথা শুনছি।"
        }

        if (cmd.contains("বন্ধ") || cmd.contains("কাটো") || cmd.contains("close") || 
            cmd.contains("exit") || cmd.contains("ব্যাক") || cmd.contains("পিছনে") || 
            cmd.contains("ক্লোজ") || cmd.contains("বন্ধ কর")) {
            
            CameraActivity.instance?.let {
                it.finish()
                return "ক্যামেরা বন্ধ করা হচ্ছে"
            }
            
            return AppHelper.closeCurrentApp(context)
        }

        if (cmd.contains("ছবি তোল") || cmd.contains("টেক ফটো") || cmd.contains("take photo") || 
            cmd.contains("ক্লিক") || cmd.contains("ছবি উঠাও")) {
            CameraActivity.instance?.let {
                it.takePhoto()
                return "ছবি তোলা হচ্ছে"
            }
        }

        return when {
            "তারিখ" in cmd || "date" in cmd -> {
                DateHelper.getCurrentDateBengali()
            }

            "ভলিউম" in cmd || "volume" in cmd || "আওয়াজ" in cmd -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if ("বাড়াও" in cmd || "up" in cmd || "increase" in cmd) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    "ভলিউম বাড়ানো হয়েছে।"
                } else if ("কমাও" in cmd || "down" in cmd || "decrease" in cmd) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    "ভলিউম কমানো হয়েছে।"
                } else {
                    "ভলিউম বাড়াতে বা কমাতে বলুন।"
                }
            }

            "চার্জ" in cmd || "ব্যাটারি" in cmd || "battery" in cmd || "charge" in cmd -> {
                "আমি ব্যাটারি লেভেল চেক করছি।"
            }

            ("গুগল" in cmd || "google" in cmd) && ("সার্চ" in cmd || "search" in cmd) -> {
                val query = extractSearchQuery(cmd, listOf("গুগল", "গুগলে", "গুগল এ", "সার্চ", "search", "on google", "google search"))
                if (query.isNotEmpty()) {
                    SearchHelper.searchInGoogle(context, query)
                } else {
                    "গুগলে কি সার্চ করতে হবে?"
                }
            }

            ("ইউটিউব" in cmd || "youtube" in cmd) && ("সার্চ" in cmd || "search" in cmd) -> {
                val query = extractSearchQuery(cmd, listOf("ইউটিউব", "ইউটিউবে", "সার্চ", "search", "on youtube", "youtube search"))
                if (query.isNotEmpty()) {
                    SearchHelper.searchInYoutube(context, query)
                } else {
                    "ইউটিউবে কি সার্চ করতে হবে?"
                }
            }

            "গুগল" in cmd || "google" in cmd || "ক্রোম" in cmd || "chrome" in cmd || "ব্রাউজার" in cmd || "browser" in cmd -> {
                "গুগলে কি সার্চ করতে হবে?"
            }
            "ইউটিউব" in cmd || "youtube" in cmd -> {
                "ইউটিউবে কি সার্চ করতে হবে?"
            }

            "ফেসবুক" in cmd || "facebook" in cmd -> AppHelper.openApp(context, listOf("com.facebook.katana", "com.facebook.lite"))
            "হোয়াটসঅ্যাপ" in cmd || "whatsapp" in cmd -> AppHelper.openApp(context, listOf("com.whatsapp", "com.whatsapp.w4b"))
            "ইনস্টাগ্রাম" in cmd || "instagram" in cmd -> AppHelper.openApp(context, listOf("com.instagram.android", "com.instagram.lite"))
            "মেসেঞ্জার" in cmd || "messenger" in cmd -> AppHelper.openApp(context, listOf("com.facebook.orca", "com.facebook.mlite"))
            "টেলিগ্রাম" in cmd || "telegram" in cmd -> AppHelper.openApp(context, listOf("org.telegram.messenger", "org.thunderdog.challegram"))

            "ক্যামেরা" in cmd || "camera" in cmd || "ছবি" in cmd -> {
                val intent = Intent(context, CameraActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "ক্যামেরা ওপেন করা হচ্ছে।"
            }

            else -> "দুঃখিত, আমি এটি বুঝতে পারছি না।"
        }
    }

    private fun extractSearchQuery(command: String, keywords: List<String>): String {
        var query = command
        for (word in keywords) {
            query = query.replace(word, "")
        }
        return query.trim().replace(Regex("\\s+"), " ")
    }

    fun isACommand(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val keywords = listOf("বন্ধ", "কাটো", "ব্যাক", "পিছনে", "ক্লোজ", "ক্যামেরা", "গুগল", "ইউটিউব", "ছবি", "চার্জ", "ভলিউম")
        return keywords.any { it in cmd }
    }
}
