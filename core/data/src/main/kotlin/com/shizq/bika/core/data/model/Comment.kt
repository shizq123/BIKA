package com.shizq.bika.core.data.model

import android.util.Log
import com.shizq.bika.core.network.model.CommentData
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class Comment(
    val id: String,
    val content: String,
    val user: User,
    val totalComments: Int = 0,
    val createdAt: String,
    val likesCount: Int,
    val isLiked: Boolean
)

fun CommentData.asExternalModel() = Comment(
    id = id,
    content = content,
    user = user.asExternalModel(),
    totalComments = totalComments,
    createdAt = formatRelativeTime(createdAt),
    likesCount = likesCount,
    isLiked = isLiked
)

/**
 * 格式化日期字符串，根据不同时间差显示不同格式。
 *
 * @param dateString ISO 8601 格式的日期字符串，例如 "2025-12-23T18:15:25.096Z"
 * @return 格式化后的相对时间字符串
 */
fun formatRelativeTime(dateString: String): String {
    return try {
        val pastInstant = Instant.parse(dateString)
        val nowInstant = Clock.System.now()

        // 如果时间在未来，直接格式化后返回
        if (pastInstant > nowInstant) {
            val dateTime = pastInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            return "${dateTime.year}年${dateTime.month.number}月${dateTime.day}日"
        }

        // 3. 计算时间差
        val duration = nowInstant - pastInstant

        // 4. 根据规则进行判断
        when {
            // a. 1分钟内 -> "刚刚"
            duration < 1.minutes -> "刚刚"

            // b. 1小时内 -> "xx分钟前"
            duration < 1.hours -> "${duration.inWholeMinutes}分钟前"

            // c. 24小时内 -> "xx小时前"
            duration < 24.hours -> "${duration.inWholeHours}小时前"

            // d. 需要使用日历天数进行比较
            else -> {
                // 获取用户设备默认时区
                val systemZone = TimeZone.currentSystemDefault()

                // 转换到本地日期进行比较
                val pastDate = pastInstant.toLocalDateTime(systemZone).date
                val nowDate = nowInstant.toLocalDateTime(systemZone).date

                val diffInDays = pastDate.daysUntil(nowDate)

                when {
                    // 昨天
                    diffInDays == 1 -> "昨天"
                    // 3天内 -> "xx天前"
                    diffInDays < 3 -> "${diffInDays}天前"
                    // 今年内 -> "M月d日"
                    pastDate.year == nowDate.year -> "${pastDate.month.number}月${pastDate.day}日"

                    // 其他（非今年） -> "yyyy年M月d日"
                    else -> "${pastDate.year}年${pastDate.month.number}月${pastDate.day}日"

                }
            }
        }
    } catch (e: IllegalArgumentException) {
        Log.e("", "Invalid date format: $dateString", e)
        dateString
    }
}