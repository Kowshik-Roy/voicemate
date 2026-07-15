package com.example.voicemate.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract

object ContactHelper {

    /**
     * কন্টাক্ট লিস্ট থেকে নাম দিয়ে নাম্বার খুঁজে বের করে।
     */
    @SuppressLint("Range")
    fun getPhoneNumberByName(context: Context, nameToSearch: String): String? {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        val queryName = nameToSearch.lowercase().trim()
        if (queryName.isEmpty()) return null

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)).lowercase()
                val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                // ১. সরাসরি মিল থাকলে
                if (contactName == queryName) return number

                // ২. কন্টাক্ট নামের ভেতরে সার্চ করা নাম থাকলে
                if (contactName.contains(queryName)) return number

                // ৩. সার্চ করা নামের ভেতরে কন্টাক্ট নাম থাকলে
                if (queryName.contains(contactName)) return number
            }
        }
        return null
    }

    /**
     * নাম্বারের শেষ কয়েকটা ডিজিট দিয়ে কন্টাক্ট খুঁজে বের করে।
     */
    @SuppressLint("Range")
    fun getPhoneNumberByLastDigits(context: Context, lastDigits: String): Pair<String, String>? {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        val digits = lastDigits.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return null

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                
                val cleanNumber = number.replace(Regex("[^0-9]"), "")
                if (cleanNumber.endsWith(digits)) {
                    return Pair(name, number)
                }
            }
        }
        return null
    }
}
