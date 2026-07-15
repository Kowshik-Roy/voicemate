package com.example.voicemate.helpers

import android.content.Context

object SmsHelper {

    fun sendSms(context: Context, phoneNumber: String, message: String): String {

        return "SMS ফিচারটি বর্তমানে বন্ধ আছে"
    }

    fun canSendSms(context: Context): Boolean {

        return true
    }
}
