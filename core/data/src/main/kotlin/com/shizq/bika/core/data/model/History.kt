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
    val lastInteractionAt: Instant,
    val categories: List<String> = emptyList(),
    val pagesCount: Int = 0,
    val epsCount: Int = 0,
    val finished: Boolean = false,
    val totalLikes: Int = 0,
    val isFavourited: Boolean = false
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
        lastInteractionAt = this.lastInteractionAt,
        categories = this.categories,
        pagesCount = this.pagesCount,
        epsCount = this.epsCount,
        finished = this.finished,
        totalLikes = this.totalLikes,
        isFavourited = this.isFavourited
    )
}

fun ChapterProgressEntity.asExternalModel(): ChapterProgress {
    return ChapterProgress(
        chapterNumber = this.chapterId,
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