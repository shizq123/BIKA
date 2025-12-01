package com.shizq.bika.core.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "historyRecord")
data class HistoryRecordEntity(
    @PrimaryKey
    val id: String,

    // 标题
    val title: String,

    // 作者或其他副标题信息
    val author: String,

    // 封面图片 URL
    val cover: String,

    // 上次阅读的时间戳
    val lastReadAt: Instant,
    // 当前章节的最大页数，可选
    val maxPage: Int?,

    // 上次阅读的进度
    @Embedded
    val lastReadProgress: ReadingProgressRecord,
)

data class ReadingProgressRecord(
    val chapterIndex: Int,
    val pageIndex: Int,
    val groupIndex: Int? = null
)