package com.shizq.bika.util

import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.model.ComicSimple

/**
 * 纯函数版本：接受已从 DB 查出的 DetailedHistory 列表（可以是实时 Flow 的最新快照），
 * 将收藏状态和阅读进度注入到 ComicSimple 列表中。
 *
 * 推荐在 ViewModel 层通过 combine(网络数据Flow, historyDao.getDetailedHistories()) 调用此函数，
 * 这样 DB 任何变化都会自动触发 UI 更新，实现真正的实时感知。
 */
fun List<ComicSimple>.injectLocalStatusFrom(histories: List<DetailedHistory>): List<ComicSimple> {
    if (histories.isEmpty()) return this
    // 建立 id -> DetailedHistory 的映射，避免对每个 comic 都线性扫描
    val historyMap = histories.associateBy { it.history.id }
    return this.map { comic ->
        val detailed = historyMap[comic.id] ?: return@map comic
        val lastProgress = detailed.progressList.maxByOrNull { it.lastReadAt }
        val progressText = computeProgressText(lastProgress, detailed.history.epsCount)
        comic.copy(
            isFavourited = detailed.history.isFavourited,
            lastReadChapterProgress = progressText
        )
    }
}

/**
 * 旧的挂起版本（一次性查询，不会实时更新）。
 * 仅在 PagingSource 的 load() 中保留，因为 PagingSource 本身不支持 Flow 响应式。
 * 对于有实时更新需求的页面，请使用 [injectLocalStatusFrom] + ViewModel combine 方案。
 */
suspend fun List<ComicSimple>.injectLocalStatus(historyDao: ReadingHistoryDao): List<ComicSimple> {
    return this.map { comic ->
        val detailedHistory = historyDao.getDetailedHistoryById(comic.id)
        if (detailedHistory != null) {
            val lastProgress = detailedHistory.progressList.maxByOrNull { it.lastReadAt }
            val progressText = computeProgressText(lastProgress, detailedHistory.history.epsCount)
            comic.copy(
                isFavourited = detailedHistory.history.isFavourited,
                lastReadChapterProgress = progressText
            )
        } else {
            comic
        }
    }
}

internal fun computeProgressText(lastProgress: ChapterProgressEntity?, epsCount: Int): String? {
    if (lastProgress == null) return null
    return when {
        epsCount > lastProgress.chapterId -> "有更新"
        lastProgress.chapterId >= epsCount
                && lastProgress.currentPage >= lastProgress.pageCount
                && lastProgress.pageCount > 0 -> "已读完"
        else -> "已阅读"
    }
}
