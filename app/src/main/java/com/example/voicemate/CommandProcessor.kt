package com.example.voicemate

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.voicemate.helpers.AppHelper
import com.example.voicemate.helpers.ContactHelper
import com.example.voicemate.helpers.DateHelper
import com.example.voicemate.service.VoiceAccessibilityService

@Suppress("SpellCheckingInspection")
object CommandProcessor {

    private val appNameMapping = mapOf(
        "গুগল" to "google", "ইউটিউব" to "youtube", "ফেসবুক" to "facebook",
        "ম্যাপ" to "maps", "প্লে স্টোর" to "play store", "গ্যালারি" to "gallery",
        "ক্যামেরা" to "camera", "সেটিংস" to "settings", "ফোন" to "phone",
        "মেসেজ" to "message", "হোয়াটসঅ্যাপ" to "whatsapp", "ম্যাসেঞ্জার" to "messenger"
    )

    private fun String.containsWord(word: String): Boolean {
        val pattern = "(^|[^a-zA-Z0-9\\u0980-\\u09FF])${Regex.escape(word)}($|[^a-zA-Z0-9\\u0980-\\u09FF])"
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(this)
    }

    private fun convertBengaliDigits(input: String): String {
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in 0..9) {
            result = result.replace(bengaliDigits[i], englishDigits[i])
        }
        return result
    }

    private fun makeCall(context: Context, number: String, name: String): String {
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            if (name == number) "$number নাম্বারে কল করা হচ্ছে" else "$name কে কল করা হচ্ছে"
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(dialIntent)
            if (name == number) "$number নাম্বারটি ডায়াল প্যাডে দেওয়া হয়েছে" else "$name এর নাম্বার ডায়াল প্যাডে দেওয়া হয়েছে"
        }
    }

    fun processCommand(context: Context, command: String): String {
        val cmd = convertBengaliDigits(command.lowercase().trim())
        
        // ১. টাইপিং মোড
        if (listOf("টাইপিং", "টাইপ", "লিখুন", "typing").any { cmd.containsWord(it) } && !cmd.contains("বন্ধ")) {
            return "ACTION_START_TYPING"
        }

        // ২. সার্চ কমান্ড (গুগল ও ইউটিউব)
        val searchKeywords = listOf("search", "সার্চ", "খুঁজো", "খুঁজুন", "দেখাও", "দেখবো", "খুঁজে দাও")
        val ytKeywords = listOf("youtube", "ইউটিউব")
        val ggKeywords = listOf("google", "গুগল")
        val openKeywords = listOf("open", "খুলো", "খোলো", "ওপেন", "চালু")
        
        val containsYt = ytKeywords.any { cmd.contains(it) }
        val containsGg = ggKeywords.any { cmd.contains(it) }
        val containsSearch = searchKeywords.any { cmd.contains(it) }

        if (containsYt || containsGg || containsSearch) {
            var targetApp = if (containsYt) "youtube" else if (containsGg) "google" else ""
            
            val noiseWords = listOf("এ", "তে", "করে", "দিয়ে", "দাও", "করো", "নিয়ে", "ইন", "in", "open", "koro", "search", "খুলো", "ওপেন", "চালু", "করুন", "লাগাও", "please", "app", "অ্যাপ", "কে", "রে")
            val toRemove = (searchKeywords + ytKeywords + ggKeywords + openKeywords + noiseWords).distinct().sortedByDescending { it.length }
            
            var query = cmd
            for (word in toRemove) {
                val pattern = "(^|[^a-zA-Z0-9\\u0980-\\u09FF])${Regex.escape(word)}($|[^a-zA-Z0-9\\u0980-\\u09FF])"
                query = query.replace(Regex(pattern, RegexOption.IGNORE_CASE), " ")
            }
            query = query.trim().replace(Regex("\\s+"), " ")

            if (query.isNotEmpty()) {
                if (targetApp.isEmpty()) targetApp = "google"
                val encodedQuery = Uri.encode(query)
                val url = if (targetApp == "youtube") "https://www.youtube.com/results?search_query=$encodedQuery"
                          else "https://www.google.com/search?q=$encodedQuery"
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return "${if(targetApp == "youtube") "ইউটিউবে" else "গুগলে"} $query সার্চ করা হচ্ছে"
                } catch (e: Exception) {
                    return "সার্চ করা সম্ভব হয়নি"
                }
            } 
            else if (targetApp.isNotEmpty()) {
                return if (targetApp == "youtube") "PROMPT_SEARCH|youtube|ইউটিউবে কি দেখতে চান?"
                       else "PROMPT_SEARCH|google|গুগলে কি সার্চ করবো?"
            }
        }

        // ৩. কল কমান্ড (কী-ওয়ার্ডসহ)
        if (cmd.contains("call") || cmd.contains("কল") || cmd.contains("ফোন")) {
            var input = cmd
            val callKeywords = listOf("call", "কল", "ফোন", "করো", "করুন", "দাও", "লাগাও", "নাম্বারে", "নম্বরে")
            callKeywords.forEach { input = input.replace(it, "") }
            val nameOrNumber = input.trim().replace(Regex("[কে|রে]$"), "").trim()

            if (nameOrNumber.isNotEmpty()) {
                val digits = nameOrNumber.replace(Regex("[^0-9]"), "")
                if (digits.length == 11) return makeCall(context, digits, digits)
                if (digits.length >= 3 && digits.length < 11) {
                    val result = ContactHelper.getPhoneNumberByLastDigits(context, digits)
                    if (result != null) return makeCall(context, result.second, result.first)
                }
                val number = ContactHelper.getPhoneNumberByName(context, nameOrNumber)
                if (number != null) return makeCall(context, number, nameOrNumber)
                return if (digits.isNotEmpty()) "নাম্বারটি সঠিক নয়" else "দুঃখিত, $nameOrNumber পাওয়া যায়নি"
            }
        }

        // ৪. সরাসরি নাম্বার বা নাম বললে কল (নতুন ফিচার)
        val cleanInput = cmd.replace(Regex("[কে|রে]$"), "").trim()
        val digitsOnly = cleanInput.replace(Regex("[^0-9]"), "")
        
        // সরাসরি ১১ ডিজিট (মাঝে স্পেস থাকলেও কাজ করবে)
        if (digitsOnly.length == 11) {
            return makeCall(context, digitsOnly, digitsOnly)
        }
        
        // সরাসরি কন্টাক্ট নাম (১-৩ শব্দের নাম হলে)
        if (cmd.split(" ").size <= 3) {
            val number = ContactHelper.getPhoneNumberByName(context, cleanInput)
            if (number != null) {
                return makeCall(context, number, cleanInput)
            }
        }

        // ৫. স্ক্রল কমান্ড
        if (listOf("up", "উপরে", "উঠো").any { cmd.containsWord(it) }) {
            VoiceAccessibilityService.instance?.scrollUp()
            return "উপরে যাওয়া হচ্ছে"
        }
        if (listOf("down", "নিচে", "নামো").any { cmd.containsWord(it) }) {
            VoiceAccessibilityService.instance?.scrollDown()
            return "নিচে যাওয়া হচ্ছে"
        }

        // ৬. সাধারণ অ্যাপ ওপেন
        var appName = cmd
        openKeywords.forEach { appName = appName.replace(it, "") }
        appName = appName.trim()

        if (appName.isNotEmpty()) {
            val mappedName = appNameMapping[appName] ?: appName
            val apps = AppHelper.getAllInstalledApps(context)
            val target = apps.find { it.name.contains(mappedName, true) || mappedName.contains(it.name, true) }
            if (target != null) return AppHelper.openApp(context, listOf(target.packageName))
        }

        if (cmd.contains("সময়") || cmd.contains("time")) return DateHelper.getCurrentDateBengali()
        if (listOf("বন্ধ", "কাটো", "exit").any { cmd.containsWord(it) }) return AppHelper.closeCurrentApp(context)

        return "UNKNOWN_COMMAND"
    }

    fun isACommand(command: String): Boolean {
        val cmd = convertBengaliDigits(command.lowercase().trim())
        val keywords = listOf(
            "কল", "call", "ফোন", "phone", "টাইপিং", "typing", "উপরে", "up", "নিচে", "down", 
            "সার্চ", "search", "ওপেন", "open", "খুলো", "খোলো", "বন্ধ", "stop", "exit", 
            "খুঁজো", "দেখাও", "দেখবো", "ইউটিউব", "youtube", "গুগল", "google"
        )
        
        // যদি কি-ওয়ার্ড থাকে
        if (keywords.any { cmd.contains(it) }) return true
        
        // যদি সরাসরি ১১ ডিজিটের নাম্বার হয়
        val digits = cmd.replace(Regex("[^0-9]"), "")
        if (digits.length == 11) return true
        
        // যদি শব্দের সংখ্যা ৩ বা তার কম হয় (সম্ভাব্য কন্টাক্ট নাম)
        if (cmd.split(" ").size <= 3) return true
        
        return false
    }

    fun isGreeting(command: String): Boolean = listOf("hello", "hi", "হ্যালো").any { cmd -> command.lowercase().contains(cmd) }
}
