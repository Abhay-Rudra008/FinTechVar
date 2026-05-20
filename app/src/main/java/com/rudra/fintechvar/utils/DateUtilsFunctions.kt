package com.rudra.fintechvar.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtilsFunctions {
    fun formatDate(time: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(time))

    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private val dateFormatter = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    private val fullFormatter = SimpleDateFormat("hh:mm a, dd MMM", Locale.ENGLISH)


    fun formatDateTime(timeInMillis: Long): String {
        return fullFormatter.format(Date(timeInMillis))
    }

    fun formatSmartDateTime(timeInMillis: Long): String {
        val timeString = timeFormatter.format(Date(timeInMillis))

        return when {
            DateUtils.isToday(timeInMillis) -> {
                "Today, $timeString"
            }

            isYesterday(timeInMillis) -> {
                "Yesterday, $timeString"
            }

            else -> {
                val dateString = dateFormatter.format(Date(timeInMillis))
                "$dateString, $timeString"
            }
        }
    }

    private fun isYesterday(timeInMillis: Long): Boolean {
        return DateUtils.isToday(timeInMillis + DateUtils.DAY_IN_MILLIS)
    }

    fun getStartOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun getStartOfWeek(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun getStartOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun getStartOfYear(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
