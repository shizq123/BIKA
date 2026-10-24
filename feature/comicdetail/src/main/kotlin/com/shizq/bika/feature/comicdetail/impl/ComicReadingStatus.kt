package com.shizq.bika.feature.comicdetail.impl

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.isCompleted

/** 阅读状态与作品是否完结独立；重读较早章节不应降低已到达的章节。 */
enum class ComicReadingStatus {
    HasUpdates,
    Completed,
    Read,
}

internal fun resolveComicReadingStatus(
    episodeCount: Int,
    progress: List<ChapterProgressEntity>,
): ComicReadingStatus? {
    val furthest = progress.filter { it.chapterId > 0 }.maxByOrNull { it.chapterId }
        ?: return null
    return when {
        episodeCount <= 0 -> ComicReadingStatus.Read
        furthest.chapterId < episodeCount -> ComicReadingStatus.HasUpdates
        furthest.chapterId == episodeCount && furthest.isCompleted -> ComicReadingStatus.Completed
        else -> ComicReadingStatus.Read
    }
}
