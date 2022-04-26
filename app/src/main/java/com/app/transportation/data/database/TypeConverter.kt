package com.app.transportation.data.database

import com.app.transportation.core.dateToString
import com.app.transportation.core.stringToDate
import java.util.*

class TypeConverter {
    @androidx.room.TypeConverter
    fun stringToDate(stringDate: String): Date = stringDate.stringToDate()

    @androidx.room.TypeConverter
    fun dateToString(date: Date): String = date.dateToString()
}


