package com.shizq.bika.utils

import java.text.SimpleDateFormat
import java.util.*

class TimestampFormat {

    fun getDateToString(milSecond: Long): String {
        val date = Date(milSecond)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }

    fun getDate(StringDate: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")//格式 2000-01-01T00:00:00+00:00
        val date=simpleDateFormat.parse(StringDate)
        val simpleDateFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")//格式 2000-01-01 00:00:00
        return simpleDateFormat2.format(date)
    }

}