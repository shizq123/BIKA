package com.shizq.bika.utils

import java.text.SimpleDateFormat
import java.util.*

class TimestampFormat {

    fun getDateToString(milSecond: Long): String {
        val date = Date(milSecond)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }
}