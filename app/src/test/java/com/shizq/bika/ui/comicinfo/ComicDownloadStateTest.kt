package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.model.DownloadTask
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 底栏"已下载"标记的判定。
 *
 * 原实现用 `downloadTasks.count { COMPLETED } >= epsCount`，
 * 而 taskId 规则是 `${comicId}_$episodeOrder`——同一章节重复入队会产生
 * 多条记录，计数把它们重复计入，章节没下全也会显示"已下载"，
 * 用户点不动下载按钮。这里按 order 去重后钉死行为。
 */
class ComicDownloadStateTest {

    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun task(
        order: Int,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        id: String = "comic1_$order",
    ) = DownloadTask(
        id = id,
        comicId = "comic1",
        comicTitle = "标题",
        coverUrl = "",
        episodeId = "ep$order",
        episodeTitle = "第 $order 话",
        episodeOrder = order,
        status = status,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun `没有任务时未下载`() {
        assertFalse(isComicFullyDownloaded(emptyList(), epsCount = 3))
    }

    @Test
    fun `章节下全时为已下载`() {
        val tasks = listOf(task(1), task(2), task(3))

        assertTrue(isComicFullyDownloaded(tasks, epsCount = 3))
    }

    @Test
    fun `章节没下全时未下载`() {
        val tasks = listOf(task(1), task(2))

        assertFalse(isComicFullyDownloaded(tasks, epsCount = 3))
    }

    @Test
    fun `重复入队的同一章节不被重复计入`() {
        // 这是要修的核心缺陷：三条记录但只覆盖两个章节
        val tasks = listOf(
            task(1, id = "comic1_1"),
            task(1, id = "comic1_1"),
            task(2, id = "comic1_2"),
        )

        assertFalse(isComicFullyDownloaded(tasks, epsCount = 3))
    }

    @Test
    fun `未完成的任务不计入`() {
        val tasks = listOf(
            task(1, status = DownloadStatus.COMPLETED),
            task(2, status = DownloadStatus.DOWNLOADING),
            task(3, status = DownloadStatus.FAILED),
        )

        assertFalse(isComicFullyDownloaded(tasks, epsCount = 3))
    }

    @Test
    fun `全部任务都未完成时未下载`() {
        val tasks = listOf(
            task(1, status = DownloadStatus.PENDING),
            task(2, status = DownloadStatus.PAUSED),
        )

        assertFalse(isComicFullyDownloaded(tasks, epsCount = 2))
    }

    @Test
    fun `完成数超过章节总数仍视为已下载`() {
        // 服务端章节数变少（下架）时不该退回"未下载"
        val tasks = listOf(task(1), task(2), task(3))

        assertTrue(isComicFullyDownloaded(tasks, epsCount = 2))
    }

    @Test
    fun `单话漫画有一条完成即为已下载`() {
        assertTrue(isComicFullyDownloaded(listOf(task(1)), epsCount = 1))
    }

    @Test
    fun `epsCount 为零且有完成任务时视为已下载`() {
        // 服务端偶发返回 0，此时不能因为除不出比例就一直显示"未下载"，
        // 否则用户会反复重新下载已有内容
        assertTrue(isComicFullyDownloaded(listOf(task(1)), epsCount = 0))
    }

    @Test
    fun `epsCount 为零且无完成任务时未下载`() {
        val tasks = listOf(task(1, status = DownloadStatus.DOWNLOADING))

        assertFalse(isComicFullyDownloaded(tasks, epsCount = 0))
    }
}
