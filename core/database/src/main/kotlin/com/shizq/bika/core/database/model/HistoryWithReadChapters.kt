package com.shizq.bika.core.database.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 用于一次性查询出历史记录及其所有已读章节的关系类。
 */
data class HistoryWithReadChapters(
    @Embedded
    val history: HistoryRecordEntity,

    @Relation(
        parentColumn = "id", // HistoryEntity 的主键
        entityColumn = "historyId" // ReadChapterEntity 的外键
    )
    val readChapters: List<ReadChapterEntity>
)