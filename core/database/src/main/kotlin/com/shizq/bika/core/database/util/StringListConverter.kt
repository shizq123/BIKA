package com.shizq.bika.core.database.util

import androidx.room.TypeConverter

internal class StringListConverter {
    @TypeConverter
    fun stringToStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(",")
    }
}
