package com.example.voicemate.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
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
     * ভয়েস থেকে প্রাপ্ত টেক্সট ইনপুট ফিল্ডে অ্যাপেন্ড (Append) করে
     */
    fun appendText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val currentText = focusedNode.text ?: ""
            val newText = if (currentText.isEmpty()) text else "$currentText $text"
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
