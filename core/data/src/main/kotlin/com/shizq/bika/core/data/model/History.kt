package com.shizq.bika.core.data.model

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import kotlin.time.Instant

data class DetailedReadingHistory(
    val history: ReadingHistory,
    val progressList: List<ChapterProgress>
) {
    val lastReadChapterProgress: ChapterProgress?
        get() = progressList.maxByOrNull { it.lastReadAt }
    val lastReadChapterProgressPercentage: Float
        get() {
            val progress = lastReadChapterProgress
            return if (progress != null && progress.pageCount > 0) {
                progress.currentPage.toFloat() / progress.pageCount
            } else {
                0f
            }
        }
}

data class ReadingHistory(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val lastInteractionAt: Instant
)

data class ChapterProgress(
    val chapterNumber: Int,
    val currentPage: Int,
    val pageCount: Int,
    val lastReadAt: Instant
)

fun ReadingHistoryEntity.asExternalModel(): ReadingHistory {
    return ReadingHistory(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl,
        lastInteractionAt = this.lastInteractionAt
    )
}

fun ChapterProgressEntity.asExternalModel(): ChapterProgress {
    return ChapterProgress(
        chapterNumber = this.chapterNumber,
        currentPage = this.currentPage,
        pageCount = this.pageCount,
        lastReadAt = this.lastReadAt
    )
}

fun DetailedHistory.asExternalModel(): DetailedReadingHistory {
    return DetailedReadingHistory(
        history = this.history.asExternalModel(),
        progressList = this.progressList.map { it.asExternalModel() }
    )
}