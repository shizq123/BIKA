package com.shizq.bika.util

import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ComicStatusInjectorTest {

    private val now = Instant.fromEpochMilliseconds(1000L)

    private fun createComic(id: String, isFavourited: Boolean = false) = ComicSummary(
        id = id,
        title = "Comic $id",
        author = "Author",
        image = RemoteImage(),
        isFavourited = isFavourited,
    )

    private fun createDetailedHistory(
        id: String,
        isFavourited: Boolean,
    ): DetailedHistory {
        val history = ReadingHistoryEntity(
            id = id,
            title = "Comic $id",
            author = "Author",
            coverUrl = "",
            lastInteractionAt = now,
            isFavourited = isFavourited,
        )
        val progress = ChapterProgressEntity(
            historyId = id,
            chapterId = 1,
            currentPage = 1,
            pageCount = 10,
            lastReadAt = now,
        )
        return DetailedHistory(history = history, progressList = listOf(progress))
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
}
