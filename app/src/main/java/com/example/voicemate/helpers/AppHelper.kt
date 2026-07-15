package com.example.voicemate.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.voicemate.service.VoiceAccessibilityService

object AppHelper {

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable? = null
    )

    /**
     * ইনস্টল করা লঞ্চার অ্যাপগুলো খুঁজে বের করে। এটি QUERY_ALL_PACKAGES ছাড়াই আধুনিক নিয়মে কাজ করে।
     */
    fun getAllInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val pm = context.packageManager
        
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val resolvedInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(mainIntent, 0)
        }

        for (info in resolvedInfos) {
            try {
                val name = info.loadLabel(pm).toString()
                val packageName = info.activityInfo.packageName
                // লিস্টিংয়ের সময় আইকন লোড করলে মেমোরি বেশি খরচ হয়, তাই প্রয়োজন হলে লোড করা ভালো।
                apps.add(AppInfo(name, packageName, null))
            } catch (e: Exception) {
                // কোনো অ্যাপের তথ্য পাওয়া না গেলে সেটি স্কিপ করবে।
            }
        }
        return apps.distinctBy { it.packageName }.sortedBy { it.name }
    }

    /**
     * ভয়েস কমান্ড অনুযায়ী অ্যাপ ওপেন করে।
     */
    fun openApp(context: Context, packageNames: List<String>): String {
        for (packageName in packageNames) {
            // চেক করা হচ্ছে সেটিংস থেকে কোনো অ্যাপ ব্লক করা কি না
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
        return "দুঃখিত, আপনার ফোনে এই অ্যাপটি খুঁজে পাওয়া যায়নি"
    }

    /**
     * হোম স্ক্রিনে ফিরে যায়।
     */
    fun closeCurrentApp(context: Context): String {
        VoiceAccessibilityService.instance?.let {
            it.goHome()
            return "হোম স্ক্রিনে যাওয়া হচ্ছে"
        }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "হোম স্ক্রিনে যাওয়া হচ্ছে"
        } catch (_: Exception) {
            "দুঃখিত, এটি করা সম্ভব হচ্ছে না"
        }
    }
}
