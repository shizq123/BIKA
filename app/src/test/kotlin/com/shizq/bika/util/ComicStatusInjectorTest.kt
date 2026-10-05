package com.shizq.bika.util

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ComicStatusInjectorTest {

    private val now = Instant.fromEpochMilliseconds(1000L)

    private fun createComic(id: String, lastProgress: String? = null, isFavourited: Boolean = false) = ComicSummary(
        id = id,
        title = "Comic $id",
        author = "Author",
        totalViews = 0,
        totalLikes = 0,
        pagesCount = 10,
        epsCount = 1,
        finished = false,
        categories = emptyList(),
        tags = emptyList(),
        image = RemoteImage("", "", ""),
        isFavourited = isFavourited,
        lastReadChapterProgress = lastProgress,
    )

    private fun createDetailedHistory(
        id: String,
        epsCount: Int,
        chapterId: Int,
        currentPage: Int,
        pageCount: Int,
        isFavourited: Boolean = false,
    ): DetailedHistory {
        val history = ReadingHistoryEntity(
            id = id,
            title = "Comic $id",
            author = "Author",
            coverUrl = "",
            lastInteractionAt = now,
            epsCount = epsCount,
            isFavourited = isFavourited,
        )
        val progress = ChapterProgressEntity(
            historyId = id,
            chapterId = chapterId,
            currentPage = currentPage,
            pageCount = pageCount,
            lastReadAt = now,
        )
        return DetailedHistory(history = history, progressList = listOf(progress))
    }

    @Test
    fun `当总章节大于已读章节时状态为有更新`() {
        val comics = listOf(createComic("1"))
        val histories = listOf(createDetailedHistory("1", epsCount = 5, chapterId = 2, currentPage = 10, pageCount = 10))

        val result = comics.injectLocalStatusFrom(histories)

        assertEquals("有更新", result.first().lastReadChapterProgress)
    }

    @Test
    fun `当已读到最新话且读完时状态为已读完`() {
        val comics = listOf(createComic("1"))
        val histories = listOf(createDetailedHistory("1", epsCount = 1, chapterId = 1, currentPage = 10, pageCount = 10, isFavourited = true))

        val result = comics.injectLocalStatusFrom(histories)

        assertEquals("已读完", result.first().lastReadChapterProgress)
        assertTrue(result.first().isFavourited)
    }

    @Test
    fun `当已读到最新话但未读完时状态为已阅读`() {
        val comics = listOf(createComic("1"))
        val histories = listOf(createDetailedHistory("1", epsCount = 1, chapterId = 1, currentPage = 2, pageCount = 20))

        val result = comics.injectLocalStatusFrom(histories)

        assertEquals("已阅读", result.first().lastReadChapterProgress)
    }

    @Test
    fun `历史记录清空时已有状态被置为null`() {
        val comics = listOf(createComic("1", lastProgress = "已读完"))

        val result = comics.injectLocalStatusFrom(emptyList())

        assertNull(result.first().lastReadChapterProgress)
    }

    @Test
    fun `单本漫画从历史中移除后其状态被置为null`() {
        val comics = listOf(
            createComic("1", lastProgress = "已读完"),
            createComic("2", lastProgress = "已阅读")
        )
        // 只保留 2 的历史，1 的历史被删除了
        val histories = listOf(createDetailedHistory("2", epsCount = 1, chapterId = 1, currentPage = 10, pageCount = 10))

        val result = comics.injectLocalStatusFrom(histories)

        assertNull(result[0].lastReadChapterProgress)
        assertEquals("已读完", result[1].lastReadChapterProgress)
    }
}
