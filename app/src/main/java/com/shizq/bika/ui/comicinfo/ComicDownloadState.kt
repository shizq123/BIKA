package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.download.model.DownloadTask

/**
 * 整本漫画是否已下载完。
 *
 * 用"已完成章节 order 的去重集合"而不是任务计数：taskId 的生成规则是
 * `${comicId}_$episodeOrder`，同一章节重复入队会产生多条记录，
 * 计数会把它们重复计入，导致章节没下全也显示"已下载"。
 *
 * @param epsCount 服务端给出的章节总数。<= 1 时按单话漫画处理：
 *   有任意一条完成即视为下载完，与原实现在这一分支上的行为一致。
 */
internal fun isComicFullyDownloaded(tasks: List<DownloadTask>, epsCount: Int): Boolean {
    val completedOrders = tasks.filter { it.isCompleted }.mapTo(mutableSetOf()) { it.episodeOrder }
    if (completedOrders.isEmpty()) return false
    // 单话漫画的 epsCount 可能是 0 或 1，此时有一条完成即视为下载完
    return if (epsCount <= 1) true else completedOrders.size >= epsCount
}
