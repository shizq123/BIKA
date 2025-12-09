package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "readingHistory")
data class ReadingHistoryEntity(
    @PrimaryKey
    val id: String,

    // 标题
    val title: String,

    // 作者或其他副标题信息
    val author: String,

    // 封面图片 URL
    val coverUrl: String,

    // 最后交互时间（用于在历史记录列表中排序）
    val lastInteractionAt: Instant
)