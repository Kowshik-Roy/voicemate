package com.example.voicemate.helpers

import android.content.Context
import android.content.Intent

object AppHelper {
    fun openApp(context: Context, packageNames: List<String>): String {
        for (packageName in packageNames) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "অ্যাপটি ওপেন করা হচ্ছে"
                }
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        return "দুঃখিত, আপনার ফোনে এই অ্যাপটি পাওয়া যায়নি"
    }

    // হোম স্ক্রিনে ফিরে যাওয়ার মাধ্যমে বর্তমান অ্যাপটি "বন্ধ" বা মিনিমাইজ করা
    fun closeCurrentApp(context: Context): String {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "অ্যাপটি বন্ধ করা হচ্ছে"
        } catch (e: Exception) {
            "দুঃখিত, এটি বন্ধ করা সম্ভব হচ্ছে না"
        }
    }
}
