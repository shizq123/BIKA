package com.shizq.bika.util

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.isCompleted
import com.shizq.bika.core.model.ComicReadingStatus
import com.shizq.bika.core.model.ComicSummary

/**
 * 将本地收藏状态与阅读进度注入漫画列表。
 *
 * 推荐在 ViewModel 层通过 combine(网络数据 Flow, historyDao.getDetailedHistories()) 调用，
 * 使数据库中的收藏与阅读记录变化能够实时反映到 UI。
 */
fun List<ComicSummary>.injectLocalStatusFrom(histories: List<DetailedHistory>): List<ComicSummary> {
    if (histories.isEmpty()) {
        return if (none { it.readingStatus != null }) this else map { it.copy(readingStatus = null) }
    }
    // 建立 id -> DetailedHistory 的映射，避免对每个 comic 都线性扫描。
    val historyMap = histories.associateBy { it.history.id }
    return map { comic -> comic.injectFromHistoryMap(historyMap) }
}

/**
 * 使用调用方预先构建的映射，为单个漫画注入本地收藏状态与阅读状态。
 */
fun ComicSummary.injectFromHistoryMap(
    historyMap: Map<String, DetailedHistory>,
): ComicSummary {
    val detailed = historyMap[id]
        ?: return if (readingStatus == null) this else copy(readingStatus = null)
    val effectiveEpsCount = if (epsCount > 0) epsCount else detailed.history.epsCount
    val status = resolveReadingStatus(effectiveEpsCount, detailed.progressList)
    return copy(
        isFavourited = detailed.history.isFavourited,
        readingStatus = status,
    )
}

/**
 * 根据作品总章节数与本地章节进度解析阅读状态；重读较早章节不会降低已到达的最远章节。
 */
internal fun resolveReadingStatus(
    episodeCount: Int,
    progressList: List<ChapterProgressEntity>,
): ComicReadingStatus? {
    val furthest = progressList.filter { it.chapterId > 0 }.maxByOrNull { it.chapterId }
        ?: return null
    return when {
        episodeCount <= 0 -> ComicReadingStatus.Read
        furthest.chapterId < episodeCount -> ComicReadingStatus.HasUpdates
        furthest.chapterId >= episodeCount && furthest.isCompleted -> ComicReadingStatus.Completed
        else -> ComicReadingStatus.Read
    }
}
