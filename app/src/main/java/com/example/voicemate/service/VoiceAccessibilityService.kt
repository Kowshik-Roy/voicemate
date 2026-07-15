package com.example.voicemate.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast

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

    fun scrollDown() {
        val path = Path()
        val metrics = resources.displayMetrics
        val middleX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.7f
        val endY = metrics.heightPixels * 0.3f
        path.moveTo(middleX, startY)
        path.lineTo(middleX, endY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun scrollUp() {
        val path = Path()
        val metrics = resources.displayMetrics
        val middleX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.3f
        val endY = metrics.heightPixels * 0.7f
        path.moveTo(middleX, startY)
        path.lineTo(middleX, endY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * ইনপুট ফিল্ড খুঁজে বের করে এবং ক্লিক করে কীবোর্ড ওপেন করে
     */
    fun focusAndOpenKeyboard() {
        val targetNode = findInputNodeInAllWindows()
        if (targetNode == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "লেখার জায়গা খুঁজে পাওয়া যাচ্ছে না", Toast.LENGTH_SHORT).show()
            }
            return
        }

        targetNode.let { node ->
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            
            // জেসচার ট্যাপের মাধ্যমে কীবোর্ড ট্রিগার করা
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val clickX = rect.centerX().toFloat()
                val clickY = rect.centerY().toFloat()
                
                val path = Path()
                path.moveTo(clickX, clickY)
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                    .build()
                dispatchGesture(gesture, null, null)
            }
            
            // ব্যাকআপ ক্লিক
            Handler(Looper.getMainLooper()).postDelayed({
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }, 100)
        }
    }

    /**
     * সকল উইন্ডো থেকে ইনপুট ফিল্ড খুঁজে বের করে (মেসেঞ্জার/হোয়াটসঅ্যাপ স্পেশাল)
     */
    fun findInputNodeInAllWindows(): AccessibilityNodeInfo? {
        val currentWindows = windows
        if (currentWindows.isNotEmpty()) {
            for (window in currentWindows) {
                val root = window.root ?: continue
                val target = findInputNodeRecursive(root)
                if (target != null) return target
            }
        }
        rootInActiveWindow?.let { return findInputNodeRecursive(it) }
        return null
    }

    private fun findInputNodeRecursive(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // ১. বর্তমানে ফোকাস থাকা ফিল্ড
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) return focused

        // ২. মেসেঞ্জার, হোয়াটসঅ্যাপ এবং অন্যান্য অ্যাপের নির্দিষ্ট আইডি
        val inputIds = listOf(
            "com.whatsapp:id/entry",                 // WhatsApp
            "com.facebook.orca:id/message_edit_text",// Messenger New
            "com.facebook.orca:id/text_input_bar",   // Messenger Old
            "com.facebook.orca:id/edit_text",        // Messenger Alt
            "com.google.android.apps.messaging:id/compose_message_text", // Messages
            "org.telegram.messenger:id/message_input" // Telegram
        )
        
        for (id in inputIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty() && nodes[0].isEditable) return nodes[0]
        }

        // ৩. জেনেরিক এডিটেবল ফিল্ড খোঁজা
        return searchEditable(rootNode)
    }

    private fun searchEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = searchEditable(child)
            if (res != null) return res
        }
        return null
    }

    /**
     * টেক্সট অ্যাপেন্ড করা (টাইপিং)
     */
    fun appendText(text: String): Boolean {
        val targetNode = findInputNodeInAllWindows() ?: return false
        
        val currentText = targetNode.text?.toString() ?: ""
        val newText = if (currentText.isEmpty()) {
            text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            val lastChar = currentText.last()
            val space = if (lastChar != ' ' && !text.startsWith(" ")) " " else ""
            "$currentText$space$text"
        }
        
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        if (success) {
            val selectionArgs = Bundle()
            selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newText.length)
            selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newText.length)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        }
        
        return success
    }

    fun clickSend(): Boolean {
        val currentWindows = windows
        for (window in currentWindows) {
            val root = window.root ?: continue
            if (trySend(root)) return true
        }
        rootInActiveWindow?.let { if (trySend(it)) return true }
        return false
    }

    private fun trySend(root: AccessibilityNodeInfo): Boolean {
        val sendIds = listOf(
            "com.whatsapp:id/send", 
            "com.facebook.orca:id/send_button", 
            "com.google.android.apps.messaging:id/send_message_button_icon"
        )
        for (id in sendIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
