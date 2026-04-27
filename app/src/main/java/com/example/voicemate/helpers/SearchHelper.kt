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
            "Searching Google for $query"
        } catch (e: Exception) {
            "Failed to search on Google."
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
            "Opening website."
        } catch (e: Exception) {
            "Failed to open website."
        }
    }
}
