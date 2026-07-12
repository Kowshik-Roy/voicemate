package com.example.voicemate.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract

object ContactHelper {

    /**
     * কন্টাক্ট লিস্ট থেকে নাম দিয়ে নাম্বার খুঁজে বের করে
     */
    @SuppressLint("Range")
    fun getPhoneNumberByName(context: Context, nameToSearch: String): String? {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                // নাম পুরোপুরি মিললে বা নামের অংশ থাকলে
                if (name.equals(nameToSearch, ignoreCase = true) || 
                    name.lowercase().contains(nameToSearch.lowercase())) {
                    return number
                }
            }
        }
        return null
    }
}
