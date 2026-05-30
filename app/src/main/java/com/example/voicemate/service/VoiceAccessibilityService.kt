package com.example.voicemate.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: VoiceAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * মেসেজ সেন্ড করার জন্য সেন্ড বাটন খুঁজে ক্লিক করে
     */
    fun clickSend() {
        val rootNode = rootInActiveWindow ?: return
        
        // ১. হোয়াটসঅ্যাপের নির্দিষ্ট আইডি চেক
        val whatsappSend = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (whatsappSend.isNotEmpty()) {
            whatsappSend[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }

        // ২. মেসেঞ্জার এবং অন্যান্য অ্যাপের জন্য স্ক্রিন সার্চ
        searchAndClickSend(rootNode)
    }

    private fun searchAndClickSend(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (node.isClickable && (
            text == "send" || text == "পাঠান" || text == "পাঠাও" || text == "প্রেরণ" ||
            contentDesc.contains("send") || contentDesc.contains("পাঠান")
        )) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        
        for (i in 0 until node.childCount) {
            if (searchAndClickSend(node.getChild(i))) return true
        }
        return false
    }

    /**
     * শেষ শব্দটি মুছে ফেলে
     */
    fun deleteLastWord() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val currentText = focusedNode.text?.toString() ?: ""
        
        if (currentText.isNotEmpty()) {
            val words = currentText.trim().split(" ")
            if (words.size > 1) {
                val newText = words.dropLast(1).joinToString(" ")
                updateInputText(focusedNode, newText)
            } else {
                updateInputText(focusedNode, "")
            }
        }
    }

    /**
     * পুরো টেক্সট মুছে ফেলে
     */
    fun clearAllText() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        updateInputText(focusedNode, "")
    }

    /**
     * বর্তমান ফোকাসড ইনপুট ফিল্ডে ক্লিক করে কীবোর্ড ওপেন করার চেষ্টা করে
     */
    fun focusAndOpenKeyboard() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focusedNode?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    /**
     * ভয়েস থেকে প্রাপ্ত টেক্সট ইনপুট ফিল্ডে সুন্দরভাবে টাইপ করে
     */
    fun appendText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        
        var input = text
        var shouldSend = false
        
        // ১. বাক্যের শেষে "পাঠাও" বা "সেন্ড" থাকলে সেটি কমান্ড হিসেবে কাজ করবে
        val sendSuffixes = listOf(" পাঠাও", " পাঠান", " সেন্ড", " send")
        for (suffix in sendSuffixes) {
            if (input.lowercase().endsWith(suffix)) {
                input = input.substring(0, input.length - suffix.length)
                shouldSend = true
                break
            }
        }

        // ২. উন্নত বাংলা পাঙ্কচুয়েশন এবং চিহ্ন সাপোর্ট
        var formattedText = input
            .replace("দাড়ি", "।").replace("দাঁড়ি", "।").replace("ফুলস্টপ", "।")
            .replace("কমা", ",").replace("প্রশ্নবোধক", "?").replace("প্রশ্ন চিহ্ন", "?")
            .replace("আশ্চর্যবোধক", "!").replace("বিস্ময়কর", "!").replace("কোলন", ":")
            .replace("সেমিকোলন", ";").replace("হাইফেন", "-")
            .replace("ব্র্যাকেট শুরু", "(").replace("ব্র্যাকেট শেষ", ")")
            .replace("নতুন লাইন", "\n")

        // কিছু কমন ইমোজি ও ফ্রেইজ সাপোর্ট
        formattedText = formattedText
            .replace("হাসি", "😊").replace("ভালোবাসা", "❤️").replace("দুঃখ", "😢")
            .replace("ধন্যবাদ", "🙏").replace("লাইক", "👍").replace("সালাম", "আসসালামু আলাইকুম")
            .trim()

        if (formattedText.isNotEmpty()) {
            val currentText = focusedNode.text?.toString() ?: ""
            val newText = when {
                currentText.isEmpty() -> formattedText
                formattedText.startsWith("।") || formattedText.startsWith(",") || 
                formattedText.startsWith("?") || formattedText.startsWith(":") || 
                formattedText.startsWith(";") || formattedText.startsWith("\n") ->
                    currentText + formattedText
                else -> "$currentText $formattedText"
            }
            updateInputText(focusedNode, newText)
        }
        
        if (shouldSend) {
            Handler(Looper.getMainLooper()).postDelayed({ clickSend() }, 500)
        }
    }

    private fun updateInputText(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        // কার্সার সবসময় শেষে রাখা
        val selectionArgs = Bundle()
        selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, text.length)
        selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
