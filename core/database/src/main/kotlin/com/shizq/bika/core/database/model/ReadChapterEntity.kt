package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "readChapters",
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val historyId: String,
    val chapterIndex: Int,
    val groupIndex: Int? = null
)