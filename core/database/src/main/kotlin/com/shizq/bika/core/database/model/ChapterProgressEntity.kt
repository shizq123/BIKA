package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.time.Instant

@Entity(
    tableName = "chapter_progress",
    primaryKeys = ["historyId", "chapterIndex"],
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

    // 章节的索引或唯一标识
    val chapterIndex: Int,

    // 当前已读页码
    val currentPage: Int,

    // 章节总页数
    val pageCount: Int,

    // 该章节的最后阅读时间
    val lastReadAt: Instant
)