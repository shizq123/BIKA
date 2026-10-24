package com.shizq.bika.feature.comicdetail.impl

import com.shizq.bika.core.database.model.ChapterProgressEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ComicReadingStatusTest {
    private val readAt = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `无阅读记录不显示状态`() {
        assertNull(resolveComicReadingStatus(10, emptyList()))
    }

    @Test
    fun `未读到最新章节显示有更新`() {
        assertEquals(
            ComicReadingStatus.HasUpdates,
            resolveComicReadingStatus(10, listOf(progress(chapterId = 9, currentPage = 20, pageCount = 20))),
        )
    }

    @Test
    fun `读到最新章节末尾显示已读完`() {
        assertEquals(
            ComicReadingStatus.Completed,
            resolveComicReadingStatus(10, listOf(progress(chapterId = 10, currentPage = 20, pageCount = 20))),
        )
    }

    @Test
    fun `读到最新章节但未读完显示已阅读`() {
        assertEquals(
            ComicReadingStatus.Read,
            resolveComicReadingStatus(10, listOf(progress(chapterId = 10, currentPage = 3, pageCount = 20))),
        )
    }

    @Test
    fun `重读旧章节仍按最远章节判断`() {
        assertEquals(
            ComicReadingStatus.Completed,
            resolveComicReadingStatus(
                10,
                listOf(
                    progress(chapterId = 10, currentPage = 20, pageCount = 20),
                    progress(chapterId = 2, currentPage = 1, pageCount = 20),
                ),
            ),
        )
    }

    @Test
    fun `章节数未知或历史超出当前章节数不误报读完`() {
        val history = listOf(progress(chapterId = 2, currentPage = 20, pageCount = 20))
        assertEquals(ComicReadingStatus.Read, resolveComicReadingStatus(0, history))
        assertEquals(ComicReadingStatus.Read, resolveComicReadingStatus(1, history))
    }

    @Test
    fun `页数未知不误报读完且无效章节被忽略`() {
        assertEquals(
            ComicReadingStatus.Read,
            resolveComicReadingStatus(1, listOf(progress(1, 0, 0))),
        )
        assertNull(resolveComicReadingStatus(1, listOf(progress(0, 20, 20))))
    }

    @Test
    fun `沿用章节末三页的完成判定`() {
        assertEquals(ComicReadingStatus.Read, resolveComicReadingStatus(1, listOf(progress(1, 16, 20))))
        assertEquals(ComicReadingStatus.Completed, resolveComicReadingStatus(1, listOf(progress(1, 17, 20))))
    }

    private fun progress(chapterId: Int, currentPage: Int, pageCount: Int) = ChapterProgressEntity(
        historyId = "comic",
        chapterId = chapterId,
        currentPage = currentPage,
        pageCount = pageCount,
        lastReadAt = readAt,
    )
}
