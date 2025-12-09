package com.shizq.bika.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 一个包含完整信息的历史记录，包括其本身和所有章节的阅读进度。
 */
data class DetailedHistory(
    @Embedded
    val history: ReadingHistoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val progressList: List<ChapterProgressEntity>
)