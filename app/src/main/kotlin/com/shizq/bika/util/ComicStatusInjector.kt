package com.shizq.bika.util

import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.model.ComicSummary

/**
 * 将本地收藏状态注入漫画列表。
 *
 * 推荐在 ViewModel 层通过 combine(网络数据 Flow, historyDao.getDetailedHistories()) 调用，
 * 使数据库中的收藏变化能够实时反映到 UI。
 */
fun List<ComicSummary>.injectLocalStatusFrom(histories: List<DetailedHistory>): List<ComicSummary> {
    if (histories.isEmpty()) return this
    // 建立 id -> DetailedHistory 的映射，避免对每个 comic 都线性扫描。
    val historyMap = histories.associateBy { it.history.id }
    return map { comic -> comic.injectFromHistoryMap(historyMap) }
}

/**
 * 使用调用方预先构建的映射，为单个漫画注入本地收藏状态。
 */
fun ComicSummary.injectFromHistoryMap(
    historyMap: Map<String, DetailedHistory>
): ComicSummary {
    val detailed = historyMap[id] ?: return this
    return copy(isFavourited = detailed.history.isFavourited)
}