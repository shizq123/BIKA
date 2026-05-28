package com.shizq.bika.util

import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.model.ComicSimple

/**
 * 通用扩展方法：从本地 Room 数据库中动态查询当前这一组 ComicSimple 列表的已收藏状态和阅读进度，
 * 并安全、流式地将它们注入数据对象中，以作为全局 Single Source of Truth 进行全局卡片展示。
 */
suspend fun List<ComicSimple>.injectLocalStatus(historyDao: ReadingHistoryDao): List<ComicSimple> {
    return this.map { comic ->
        val detailedHistory = historyDao.getDetailedHistoryById(comic.id)
        if (detailedHistory != null) {
            val lastProgress = detailedHistory.progressList.maxByOrNull { it.lastReadAt }
            val progressText = if (lastProgress != null) {
                val epsCount = detailedHistory.history.epsCount
                if (epsCount > lastProgress.chapterId) {
                    "有更新"
                } else if (lastProgress.chapterId >= epsCount && lastProgress.currentPage >= lastProgress.pageCount && lastProgress.pageCount > 0) {
                    "已读完"
                } else {
                    "已阅读"
                }
            } else {
                null
            }
            comic.copy(
                isFavourited = detailedHistory.history.isFavourited,
                lastReadChapterProgress = progressText
            )
        } else {
            comic
        }
    }
}
