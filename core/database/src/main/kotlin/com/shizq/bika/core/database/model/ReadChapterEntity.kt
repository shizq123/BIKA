package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "readChapters",
    primaryKeys = ["historyId", "chapterIndex"],
    foreignKeys = [
        ForeignKey(
            entity = HistoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadChapterEntity(
    val historyId: String,
    val chapterIndex: Int,

    val groupIndex: Int? = null
)