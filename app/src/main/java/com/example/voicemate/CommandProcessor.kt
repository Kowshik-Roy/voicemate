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
        // পাঙ্কচুয়েশন সরিয়ে এবং অতিরিক্ত স্পেস ট্রিম করে কমান্ডটি ক্লিন করা
        val cmd = command.lowercase().replace(Regex("[^a-zA-Z0-9\\u0980-\\u09FF\\s]"), " ").trim()
        val words = cmd.split(Regex("\\s+"))

        // --- 1. Close/Exit Handler (Highest Priority) ---
        val closeKeywords = listOf("বন্ধ", "বন্ধ করো", "বন্ধ কর", "কাটো", "close", "exit", "stop", "বাহির", "বের হও", "ব্যাক", "পিছনে", "quit", "কেটে দাও", "কেটে", "বের", "bondho", "exit app")
        if (closeKeywords.any { it in cmd }) {
            // যদি ক্যামেরা খোলা থাকে
            CameraActivity.instance?.let {
                it.finish()
                return "ক্যামেরা বন্ধ করা হচ্ছে"
            }
            // হোম স্ক্রিনে ফিরে আসার জন্য (Accessibility Service প্রয়োজন)
            return AppHelper.closeCurrentApp(context)
        }

        // --- 2. Telegram Open Handler (Special Case) ---
        if (cmd.contains("telegram") || cmd.contains("টেলিগ্রাম") || cmd.contains("টেলীগ্রাম") || cmd.contains("টেলি গ্রাম") || cmd.contains("টেলি")) {
            if (cmd.contains("ওপেন") || cmd.contains("খুলো") || cmd.contains("খোলো") || cmd.contains("চালু") || cmd.contains("open") || words.size <= 2) {
                val installedApps = AppHelper.getAllInstalledApps(context)
                val telegramApp = installedApps.find { 
                    it.packageName.contains("telegram") || 
                    it.packageName.contains("thunderdog") || 
                    it.packageName.contains("plus.messenger") ||
                    it.packageName.contains("challegram") ||
                    it.name.lowercase().contains("telegram") 
                }
                telegramApp?.let {
                    return AppHelper.openApp(context, listOf(it.packageName))
                }
            }
        }

        // --- 3. Camera Open Handler ---
        val cameraKeywords = listOf("camera", "ক্যামেরা")
        if (cameraKeywords.any { it in cmd }) {
            if (cmd.contains("ওপেন") || cmd.contains("খুলো") || cmd.contains("খোলো") || 
                cmd.contains("চালু") || cmd.contains("open") || words.size == 1) {
                
                val intent = Intent(context, CameraActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "ক্যামেরা ওপেন করা হচ্ছে"
            }
        }

        // --- 4. YouTube & Google Search Handler ---
        if (cmd.contains("youtube") || cmd.contains("ইউটিউব")) {
            if (words.size <= 3 && !cmd.contains("open") && !cmd.contains("খুলো") && !cmd.contains("খোলো")) {
                return "ইউটিউবে কি সার্চ করতে হবে?"
            }
        }
        if (cmd.contains("google") || cmd.contains("গুগল")) {
            if (words.size <= 3 && !cmd.contains("open") && !cmd.contains("খুলো") && !cmd.contains("খোলো")) {
                return "গুগলে কি সার্চ করতে হবে?"
            }
        }
        
        // --- 5. Greetings ---
        if (isGreeting(cmd)) {
            return "হ্যালো, আমি আপনার কথা শুনছি।"
        }

        // --- 6. Typing Mode ---
        if (cmd.contains("টাইপিং") || cmd.contains("typing") || cmd.contains("লিখতে চাই") || cmd.contains("লিখো")) {
            return if (VoiceAccessibilityService.instance == null) {
                "টাইপিং মোড ব্যবহারের জন্য অ্যাক্সেসিবিলিটি সার্ভিস অন করতে হবে। অ্যাপের হোম পেজ থেকে পারমিশন দিন।"
            } else {
                "টাইপিং মোড চালু করা হয়েছে। আপনি যা বলবেন তা টাইপ করা হবে।"
            }
        }

        // --- 7. Send Message & Edit Handler ---
        val sendKeywords = listOf("send", "পাঠাও", "মেসেজ পাঠাও", "পাঠিয়ে দাও", "সেন্ড", "send message", "পাঠান", "সেন্ড করো")
        if (sendKeywords.any { cmd == it } || (sendKeywords.any { it in cmd } && !cmd.contains("open") && !cmd.contains("খুলো") && !cmd.contains("খোলো"))) {
            return if (VoiceAccessibilityService.instance == null) {
                "মেসেজ পাঠানোর জন্য অ্যাক্সেসিবিলিটি সার্ভিস অন থাকা প্রয়োজন।"
            } else {
                VoiceAccessibilityService.instance?.clickSend()
                "মেসেজ পাঠানো হচ্ছে"
            }
        }

        // টেক্সট মুছে ফেলার কমান্ড
        if (cmd == "মুছে ফেলো" || cmd == "মুছে ফেল" || cmd == "delete") {
            VoiceAccessibilityService.instance?.deleteLastWord()
            return "শেষ শব্দ মুছে ফেলা হয়েছে"
        }
        if (cmd == "সব মুছে ফেলো" || cmd == "clear all") {
            VoiceAccessibilityService.instance?.clearAllText()
            return "সব টেক্সট মুছে ফেলা হয়েছে"
        }

        // --- 8. Date & Volume ---
        if (cmd.contains("তারিখ") || cmd.contains("date") || cmd.contains("সময়") || cmd.contains("সময়") || cmd.contains("time")) {
            return DateHelper.getCurrentDateBengali()
        }
        
        if (cmd.contains("ভলিউম") || cmd.contains("volume") || cmd.contains("সাউন্ড") || cmd.contains("sound")) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return if (cmd.contains("বাড়াও") || cmd.contains("up") || cmd.contains("বেশি")) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "ভলিউম বাড়ানো হয়েছে।"
            } else {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "ভলিউম কমানো হয়েছে।"
            }
        }

        // --- 9. Dynamic App Opener ---
        val openKeywords = listOf("open", "খুলো", "খোলো", "ওপেন", "start", "খোল", "দেখা", "চালু")
        val commonAppsMap = mapOf(
            "facebook" to listOf("facebook", "ফেসবুক", "ফেইসবুক"),
            "messenger" to listOf("messenger", "মেসেঞ্জার"),
            "whatsapp" to listOf("whatsapp", "হোয়াটসঅ্যাপ", "ওয়াটসঅ্যাপ"),
            "telegram" to listOf("telegram", "টেলিগ্রাম", "টেলীগ্রাম", "টেলি গ্রাম", "টেলিক্রাম", "thunderdog", "plus.messenger", "org.telegram"),
            "gmail" to listOf("gmail", "জিমেইল"),
            "chrome" to listOf("chrome", "ক্রোম"),
            "settings" to listOf("settings", "সেটিংস", "সেটিং")
        )

        val installedApps = AppHelper.getAllInstalledApps(context)
        var matchedApp: AppHelper.AppInfo? = null

        // কমন অ্যাপ ম্যাচিং
        for ((key, variations) in commonAppsMap) {
            if (variations.any { it in cmd }) {
                matchedApp = installedApps.find { 
                    it.packageName.lowercase().contains(key) || 
                    it.name.lowercase().contains(key)
                }
                if (matchedApp != null) break
            }
        }

        // জেনারিক ম্যাচিং
        if (matchedApp == null && (openKeywords.any { it in cmd } || words.size <= 2)) {
            var cleanedName = cmd
            openKeywords.forEach { cleanedName = cleanedName.replace(it, "") }
            cleanedName = cleanedName.trim()

            if (cleanedName.isNotEmpty()) {
                matchedApp = installedApps.find { 
                    val name = it.name.lowercase().replace(" ", "")
                    val target = cleanedName.replace(" ", "")
                    name == target || name.contains(target) || target.contains(name) || it.packageName.contains(target)
                }
            }
        }

        if (matchedApp != null) {
            if (AppPreferenceHelper.isAppAllowed(context, matchedApp.packageName)) {
                return AppHelper.openApp(context, listOf(matchedApp.packageName))
            } else {
                return "${matchedApp.name} ওপেন করার অনুমতি নেই। সেটিংস থেকে পারমিশন দিন।"
            }
        }

        return "দুঃখিত, আমি এটি বুঝতে পারছি না।"
    }

    fun isACommand(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val keywords = listOf(
            "বন্ধ", "বন্ধ করো", "বন্ধ কর", "কাটো", "কেটে দাও", "ব্যাক", "খুলো", "খোলো", "ওপেন", "open", "চালু", "start",
            "গুগল", "google", "ইউটিউবে", "youtube", "ফেসবুক", "facebook", "মেসেঞ্জার", "messenger",
            "চার্জ", "ভলিউম", "সাউন্ড", "টাইপিং", "তারিখ", "সময়", "টাইম", "whatsapp", "জিমেইল", "gmail", 
            "telegram", "টেলিগ্রাম", "টেলীগ্রাম", "টেলি গ্রাম", "ফেইসবুক", "বাহির", "কেটে", "বের হও", "camera", "ক্যামেরা", 
            "close", "exit", "stop", "quit", "send", "পাঠাও", "মেসেজ পাঠাও", "সেন্ড", "মুছে ফেলো", "মুছে ফেল", "delete", "clear all", "bondho", "exit app"
        )
        return keywords.any { it in cmd }
    }

    fun isGreeting(command: String): Boolean {
        val cmd = command.lowercase().trim()
        val greetings = listOf("hello", "hi", "হ্যালো", "হাই")
        return greetings.any { it in cmd }
    }
}
