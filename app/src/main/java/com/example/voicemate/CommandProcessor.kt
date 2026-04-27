package com.example.voicemate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import android.provider.MediaStore
import com.example.voicemate.camera.CameraActivity
import com.example.voicemate.helpers.AppHelper
import com.example.voicemate.helpers.DateHelper
import com.example.voicemate.helpers.CallHelper
import com.example.voicemate.helpers.ContactHelper

object CommandProcessor {

    fun processCommand(context: Context, command: String): String {
        val cmd = command.lowercase().trim()
        
        // হ্যালো বা হাই চেক করার জন্য আরও শক্তিশালী লজিক
        if (cmd.contains("hello") || cmd.contains("hi") || cmd.contains("hey") || 
            cmd.contains("হ্যালো") || cmd.contains("হাই") || cmd.contains("হে")) {
            return "হ্যালো, আমি আপনার কথা শুনছি।"
        }

        return when {
            // তারিখের কমান্ড
            "তারিখ" in cmd || "date" in cmd -> {
                DateHelper.getCurrentDateBengali()
            }

            // কল করার কমান্ড (নম্বর বা কন্টাক্ট)
            "কল" in cmd || "ফোন" in cmd || "call" in cmd || "phone" in cmd -> {
                val digits = cmd.filter { it.isDigit() }
                if (digits.length >= 11) {
                    CallHelper.makeCall(context, digits)
                } else if (digits.length == 3) {
                    // শেষ ৩ ডিজিট দিয়ে কন্টাক্ট সার্চ
                    val foundNumber = ContactHelper.findNumberByLastThreeDigits(context, digits)
                    if (foundNumber != null) {
                        CallHelper.makeCall(context, foundNumber)
                    } else {
                        "দুঃখিত, এই ৩ ডিজিটের কোনো নম্বর পাওয়া যায়নি।"
                    }
                } else {
                    "দুঃখিত, সঠিক মোবাইল নম্বর বা শেষ ৩টি সংখ্যা বলুন।"
                }
            }

            // ভলিউম কন্ট্রোল
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

            // চার্জ জানার কমান্ড
            "চার্জ" in cmd || "ব্যাটারি" in cmd || "battery" in cmd || "charge" in cmd -> {
                "আমি ব্যাটারি লেভেল চেক করছি।" // ব্যাটারি রিসিভার অটোমেটিক বলবে
            }

            // বন্ধ করার কমান্ড
            "বন্ধ করো" in cmd || "কাটো" in cmd || "close" in cmd || "exit" in cmd -> {
                AppHelper.closeCurrentApp(context)
            }

            // ইউটিউব এবং ক্রোম
            "ইউটিউব" in cmd || "youtube" in cmd -> "কি সার্চ করতে হবে?"
            "ক্রোম" in cmd || "chrome" in cmd || "ব্রাউজার" in cmd || "browser" in cmd -> "কি সার্চ করতে হবে?"

            // অ্যাপ ওপেন লজিক
            "ফেসবুক" in cmd || "facebook" in cmd -> AppHelper.openApp(context, listOf("com.facebook.katana", "com.facebook.lite"))
            "হোয়াটসঅ্যাপ" in cmd || "whatsapp" in cmd -> AppHelper.openApp(context, listOf("com.whatsapp", "com.whatsapp.w4b"))
            "ইনস্টাগ্রাম" in cmd || "instagram" in cmd -> AppHelper.openApp(context, listOf("com.instagram.android", "com.instagram.lite"))
            "মেসেঞ্জার" in cmd || "messenger" in cmd -> AppHelper.openApp(context, listOf("com.facebook.orca", "com.facebook.mlite"))
            "টেলিগ্রাম" in cmd || "telegram" in cmd -> AppHelper.openApp(context, listOf("org.telegram.messenger", "org.thunderdog.challegram"))

            "গুগল" in cmd || "google" in cmd -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "গুগল ওপেন করা হচ্ছে।"
            }

            "ক্যামেরা" in cmd || "camera" in cmd || "ছবি" in cmd -> {
                try {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "ক্যামেরা ওপেন করা হচ্ছে।"
                } catch (e: Exception) {
                    val intent = Intent(context, CameraActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "ক্যামেরা ওপেন করা হচ্ছে।"
                }
            }

            else -> "দুঃখিত, আমি এটি বুঝতে পারছি না।"
        }
    }

    fun isACommand(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val keywords = listOf("ফেসবুক", "facebook", "ক্রোম", "chrome", "ইউটিউব", "youtube", "ক্যামেরা", "camera", "তারিখ", "date", "হোয়াটসঅ্যাপ", "whatsapp", "ইনস্টাগ্রাম", "instagram", "মেসেঞ্জার", "messenger", "টেলিগ্রাম", "telegram", "গুগল", "google", "ভলিউম", "volume", "বন্ধ করো", "close", "কল", "ফোন", "call", "phone", "চার্জ", "battery")
        return keywords.any { it in cmd }
    }
}
