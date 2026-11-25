package com.shizq.bika.util

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.model.ComicReadingStatus
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ComicStatusInjectorTest {

    private val now = Instant.fromEpochMilliseconds(1000L)

    private fun createComic(
        id: String,
        epsCount: Int = 1,
        isFavourited: Boolean = false,
        readingStatus: ComicReadingStatus? = null,
    ) = ComicSummary(
        id = id,
        title = "Comic $id",
        author = "Author",
        epsCount = epsCount,
        image = RemoteImage(),
        isFavourited = isFavourited,
        readingStatus = readingStatus,
    )

    private fun createDetailedHistory(
        id: String,
        isFavourited: Boolean = false,
        epsCount: Int = 1,
        progressList: List<ChapterProgressEntity> = listOf(
            ChapterProgressEntity(
                historyId = id,
                chapterId = 1,
                currentPage = 1,
                pageCount = 10,
                lastReadAt = now,
            ),
        ),
    ): DetailedHistory {
        val history = ReadingHistoryEntity(
            id = id,
            title = "Comic $id",
            author = "Author",
            coverUrl = "",
            epsCount = epsCount,
            lastInteractionAt = now,
            isFavourited = isFavourited,
        )
        return DetailedHistory(history = history, progressList = progressList)
    }

    @Test
    fun `history favorite state is injected into comic list`() {
        val result = listOf(createComic("1")).injectLocalStatusFrom(
            listOf(createDetailedHistory("1", isFavourited = true)),
        )

        assertTrue(result.first().isFavourited)
    }

    @Test
    fun `unfavorited history clears stale favorite state`() {
        val result = listOf(createComic("1", isFavourited = true)).injectLocalStatusFrom(
            listOf(createDetailedHistory("1", isFavourited = false)),
        )

        assertFalse(result.first().isFavourited)
    }

    @Test
    fun `empty history keeps network favorite state while local data is unavailable`() {
        val result = listOf(createComic("1", isFavourited = true)).injectLocalStatusFrom(emptyList())

        assertTrue(result.first().isFavourited)
    }

    @Test
    fun `comic missing from history keeps network favorite state`() {
        val result = listOf(createComic("1", isFavourited = true)).injectLocalStatusFrom(
            listOf(createDetailedHistory("2", isFavourited = true)),
        )

        assertTrue(result.first().isFavourited)
    }

    @Test
    fun `最新章节数大于已读最远章节时标记为有更新`() {
        val result = listOf(createComic("1", epsCount = 5)).injectLocalStatusFrom(
            listOf(
                createDetailedHistory(
                    id = "1",
                    epsCount = 2,
                    progressList = listOf(
                        ChapterProgressEntity("1", chapterId = 2, currentPage = 10, pageCount = 10, lastReadAt = now),
                    ),
                ),
            ),
        )

        assertEquals(ComicReadingStatus.HasUpdates, result.first().readingStatus)
    }

    @Test
    fun `读到最新话且完成时标记为已读完且重读旧话不降级`() {
        val result = listOf(createComic("1", epsCount = 3)).injectLocalStatusFrom(
            listOf(
                createDetailedHistory(
                    id = "1",
                    epsCount = 3,
                    progressList = listOf(
                        ChapterProgressEntity("1", chapterId = 1, currentPage = 2, pageCount = 20, lastReadAt = Instant.fromEpochMilliseconds(2000L)),
                        ChapterProgressEntity("1", chapterId = 3, currentPage = 18, pageCount = 20, lastReadAt = now),
                    ),
                ),
            ),
        )

        assertEquals(ComicReadingStatus.Completed, result.first().readingStatus)
    }

    @Test
    fun `读到最新话未完成时标记为已阅读`() {
        val result = listOf(createComic("1", epsCount = 3)).injectLocalStatusFrom(
            listOf(
                createDetailedHistory(
                    id = "1",
                    epsCount = 3,
                    progressList = listOf(
                        ChapterProgressEntity("1", chapterId = 3, currentPage = 5, pageCount = 20, lastReadAt = now),
                    ),
                ),
            ),
        )

        assertEquals(ComicReadingStatus.Read, result.first().readingStatus)
    }

    @Test
    fun `仅有详情访问记录无章节阅读进度时不显示阅读状态`() {
        val result = listOf(createComic("1", epsCount = 3)).injectLocalStatusFrom(
            listOf(createDetailedHistory(id = "1", epsCount = 3, progressList = emptyList())),
        )

        assertNull(result.first().readingStatus)
    }

    @Test
    fun `历史记录被清空或移除时清除旧阅读状态`() {
        val comics = listOf(
            createComic("1", readingStatus = ComicReadingStatus.Completed),
            createComic("2", readingStatus = ComicReadingStatus.Read),
        )

        val clearedAll = comics.injectLocalStatusFrom(emptyList())
        assertNull(clearedAll[0].readingStatus)
        assertNull(clearedAll[1].readingStatus)

        val removedOne = comics.injectLocalStatusFrom(listOf(createDetailedHistory("2")))
        assertNull(removedOne[0].readingStatus)
        assertEquals(ComicReadingStatus.Read, removedOne[1].readingStatus)
    }
}
