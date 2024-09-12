package com.shizq.bika.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


@SuppressLint("SimpleDateFormat")
class TimeUtil {

    private fun getDate(date: Date): String {
        //传入的时间
        val calendar = Calendar.getInstance()
        calendar.time = date

        //秒
        val second = Calendar.getInstance()
        second.add(Calendar.SECOND, -60)
        if (second <= calendar) {
            second.add(Calendar.SECOND, 60)
            return ((second.timeInMillis - calendar.timeInMillis) / 1000).toString() + "秒前"
        }

        //分
        val minute = Calendar.getInstance()
        minute.add(Calendar.MINUTE, -60)
        if (minute <= calendar) {
            minute.add(Calendar.MINUTE, 60)
            return ((minute.timeInMillis - calendar.timeInMillis) / 60000).toString() + "分钟前"
        }

        //今天
        val day = Calendar.getInstance()
        if (day[Calendar.YEAR] == calendar[Calendar.YEAR]
            && day[Calendar.DAY_OF_YEAR] == calendar[Calendar.DAY_OF_YEAR]
        ) {
            val simpleDateFormat2 = SimpleDateFormat("HH:mm")
            return "今天 " + simpleDateFormat2.format(calendar.time)
        }

        //其他时间
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return if (yesterday[Calendar.YEAR] == calendar[Calendar.YEAR]
            && yesterday[Calendar.DAY_OF_YEAR] == calendar[Calendar.DAY_OF_YEAR]
        ) {
            "昨天 " + SimpleDateFormat("HH:mm").format(calendar.time)
        } else if (Calendar.getInstance()[Calendar.YEAR] == calendar[Calendar.YEAR]) {
            SimpleDateFormat("M月d日 HH:mm").format(calendar.time)
        } else {
            SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(calendar.time)
        }
    }

    fun time(stringDate: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return getDate(simpleDateFormat.parse(stringDate)!!)
    }

    fun getDate(milSecond: Long): String {
        val date = Date(milSecond)
        return getDate(date)
    }

    //2000-01-01T00:00:00+00:00 转换成 2000-01-01 00:00:00
    fun getDate(StringDate: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        val date = simpleDateFormat.parse(StringDate)
        val simpleDateFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return simpleDateFormat2.format(date!!)
    }

    //2000-01-01T00:00:00.000Z 转换成 2000-01-01
    fun getBirthday(StringDate: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val date = simpleDateFormat.parse(StringDate)
        val simpleDateFormat2 = SimpleDateFormat("yyyy-MM-dd")
        return simpleDateFormat2.format(date!!)
    }

    fun registerDays(day: Int): Boolean {
        val createdAt = SPUtil.get("user_created_at", "") as String
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val time = simpleDateFormat.parse(createdAt)!!.time
        val currentTime = System.currentTimeMillis()
        return (currentTime - time) / (1000 * 60 * 60 * 24) >= day
    }
}