package com.edoctor.dlvn_sdk.helper

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WidgetUtils {
    fun isToday(inputDateString: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(inputDateString)
        val today = Calendar.getInstance().apply { time = Date() }
        val calendar = Calendar.getInstance().apply {
            if (date != null) {
                time = date
                add(Calendar.HOUR, 7)
            }
        }

        return  today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getTime(inputDateString: String): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(inputDateString)
        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.time = date
            calendar.add(Calendar.HOUR, 7)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return calendar.time.let { timeFormat.format(it) }
    }

    fun getAnotherDayTime(inputDateString: String): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(inputDateString)
        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.time = date
            calendar.add(Calendar.HOUR, 7)
        }

        val dateFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        return calendar.time.let { dateFormat.format(it) }
    }
}