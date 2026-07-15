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
        // যদি ইউজার ম্যানুয়ালি কোনো অ্যাপ ব্লক করে থাকে তবে সেটি চেক করবে, 
        // নতুবা ডিফল্টভাবে সব অ্যাপ এলাও থাকবে।
        return getPrefs(context).getBoolean(packageName, true)
    }
}
