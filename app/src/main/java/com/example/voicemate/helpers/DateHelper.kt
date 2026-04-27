package com.example.voicemate.helpers

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateHelper {
    fun getCurrentDateBengali(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
        return "আজকের তারিখ হলো " + sdf.format(Date())
    }
}
