package com.shizq.bika.feature.comicdetail.impl

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
sealed interface ComicDownloadState {
    data object NotDownloaded : ComicDownloadState

    data class Partial(
        val completedChapters: Int,
        val expectedChapters: Int,
    ) : ComicDownloadState

    data class Downloaded(val completedChapters: Int) : ComicDownloadState
}

/**
 * 将底层章节任务聚合成漫画级下载状态。
 *
 * UI 不再理解任务去重和单话规则；其他页面需要展示下载状态时也必须复用这里。
 */
internal fun resolveComicDownloadState(
    tasks: List<DownloadTask>,
    epsCount: Int,
): ComicDownloadState {
    val completedCount = tasks
        .asSequence()
        .filter { it.isCompleted }
        .map { it.episodeOrder }
        .distinct()
        .count()

    if (completedCount == 0) return ComicDownloadState.NotDownloaded
    if (epsCount <= 1 || completedCount >= epsCount) {
        return ComicDownloadState.Downloaded(completedChapters = completedCount)
    }
    return ComicDownloadState.Partial(
        completedChapters = completedCount,
        expectedChapters = epsCount,
    )
}

internal fun isComicFullyDownloaded(tasks: List<DownloadTask>, epsCount: Int): Boolean =
    resolveComicDownloadState(tasks, epsCount) is ComicDownloadState.Downloaded

