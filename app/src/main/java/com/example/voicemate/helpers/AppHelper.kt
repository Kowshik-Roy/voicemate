package com.example.voicemate.helpers

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.example.voicemate.service.VoiceAccessibilityService

object AppHelper {

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable
    )

    fun getAllInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        for (packageInfo in packages) {
            val launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName)
            if (launchIntent != null) {
                val name = packageInfo.applicationInfo.loadLabel(pm).toString()
                val icon = packageInfo.applicationInfo.loadIcon(pm)
                apps.add(AppInfo(name, packageInfo.packageName, icon))
            }
        }
        return apps.sortedBy { it.name }
    }

    fun openApp(context: Context, packageNames: List<String>): String {
        for (packageName in packageNames) {
            // Check if the app is allowed by the user in settings
            if (!AppPreferenceHelper.isAppAllowed(context, packageName)) {
                continue
            }

            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "অ্যাপটি ওপেন করা হচ্ছে"
                }
            } catch (_: Exception) {
            }
        }
        
        // If we tried multiple packages and none were allowed or found
        val isAnyAppBlocked = packageNames.any { !AppPreferenceHelper.isAppAllowed(context, it) }
        return if (isAnyAppBlocked) {
            "অ্যাপটি ওপেন করার অনুমতি নেই। সেটিংস থেকে পারমিশন দিন।"
        } else {
            "দুঃখিত, আপনার ফোনে এই অ্যাপটি পাওয়া যায়নি"
        }
    }

    fun closeCurrentApp(context: Context): String {
        // 1. Use Accessibility Service if available (More reliable)
        VoiceAccessibilityService.instance?.let {
            it.goHome()
            return "অ্যাপটি বন্ধ করা হচ্ছে"
        }

        // 2. Fallback to Home Intent
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "অ্যাপটি বন্ধ করা হচ্ছে"
        } catch (_: Exception) {
            "দুঃখিত, এটি বন্ধ করা সম্ভব হচ্ছে না"
        }
    }
}
