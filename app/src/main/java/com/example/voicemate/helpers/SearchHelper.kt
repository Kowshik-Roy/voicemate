package com.example.voicemate.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri

object SearchHelper {

    fun searchInGoogle(context: Context, query: String): String {
        return try {
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "গুগলে $query সার্চ করা হচ্ছে"
        } catch (e: Exception) {
            "গুগল সার্চ করতে ব্যর্থ হয়েছে"
        }
    }

    fun searchInYoutube(context: Context, query: String): String {
        return try {
            val url = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "ইউটিউবে $query সার্চ করা হচ্ছে"
        } catch (e: Exception) {
            "ইউটিউব সার্চ করতে ব্যর্থ হয়েছে"
        }
    }

    fun openWebsite(context: Context, url: String): String {
        return try {
            val finalUrl = if (
                url.startsWith("http://") || url.startsWith("https://")
            ) {
                url
            } else {
                "https://$url"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "ওয়েবসাইট ওপেন করা হচ্ছে"
        } catch (e: Exception) {
            "ওয়েবসাইট ওপেন করতে ব্যর্থ হয়েছে"
        }
    }
}
