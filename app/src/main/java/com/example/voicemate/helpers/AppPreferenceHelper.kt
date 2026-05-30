package com.example.voicemate.helpers

import android.content.Context
import android.content.SharedPreferences

object AppPreferenceHelper {
    private const val PREF_NAME = "AppPermissions"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setAppPermission(context: Context, packageName: String, allowed: Boolean) {
        getPrefs(context).edit().putBoolean(packageName, allowed).apply()
    }

    fun isAppAllowed(context: Context, packageName: String): Boolean {
        // 1. If user has manually set a preference in Settings
        if (getPrefs(context).contains(packageName)) {
            return getPrefs(context).getBoolean(packageName, false)
        }

        // 2. Automatically allow common popular apps
        val autoAllowedKeywords = listOf(
            "telegram", "thunderdog", "challegram", "facebook", "whatsapp", "messenger", 
            "youtube", "gmail", "chrome", "google.android.apps.maps", "android.settings", "plus.messenger"
        )
        
        return autoAllowedKeywords.any { packageName.lowercase().contains(it) }
    }
}
