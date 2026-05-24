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
        // ডিফল্টভাবে ফেসবুক, ইউটিউব ইত্যাদি এলাউড রাখতে পারেন অথবা সব অফ রাখতে পারেন
        return getPrefs(context).getBoolean(packageName, false)
    }
}
