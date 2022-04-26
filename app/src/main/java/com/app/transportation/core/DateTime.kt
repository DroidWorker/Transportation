package com.app.transportation.core

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

val currentDateTime: Date
    get() = Calendar.getInstance().time

val currentTime
    get() = Calendar.getInstance().run { Pair(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE)) }

fun Date.dateToString(format: String = "yyyy/MM/dd HH:mm:ss",
                      locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat(format, locale).format(this)

fun String.stringToDate(format: String = "dd/MM/yyyy HH:mm",
                        locale: Locale = Locale.getDefault()): Date =
    SimpleDateFormat(format, locale).parse(this) ?: Date()

fun currentDateToString(format: String = "yyyy/MM/dd HH:mm:ss") =
    currentDateTime.dateToString(format)

fun Long.millisToDate(): Date = Calendar.getInstance().also { it.timeInMillis = this }.time

fun Long.millisToString(format: String): String = millisToDate().dateToString(format)

fun String.toMillis(): Long = stringToDate("yyyy/MM/dd HH:mm").time

fun Context.formatDate(_date: String, format: String = "yyyy/MM/dd HH:mm:ss",
                       alwaysShowTime: Boolean = false): String {
    return if (_date.isNotBlank()) {
        val date = _date.stringToDate(format)
        date.dateToString("dd MMMM yyyy HH:mm")
    } else ""
}