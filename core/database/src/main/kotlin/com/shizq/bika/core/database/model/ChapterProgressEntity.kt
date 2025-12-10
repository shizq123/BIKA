package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.time.Instant

@Entity(
    tableName = "chapterProgress",
    primaryKeys = ["historyId", "chapterId"],
    foreignKeys = [
        ForeignKey(
            entity = ReadingHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChapterProgressEntity(
    // 关联到 ReadingHistoryEntity 的外键
    val historyId: String,

    val chapterId: String,

    // 用于界面显示（"第5话"）和数据库排序
    val chapterNumber: Int,
    // 当前已读页码
    val currentPage: Int,

    // 章节总页数
    val pageCount: Int,

    // 该章节的最后阅读时间
    val lastReadAt: Instant
)