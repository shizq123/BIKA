package com.shizq.bika.core.data.paging

import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.PageData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChapterListLoaderTest {
    @Test
    fun `only the final catalog snapshot is complete`() = runTest {
        val collected = mutableListOf<Chapter>()
        val snapshots = mutableListOf<ChapterCatalog>()
        walkEpisodePages(
            fetchPage = { page -> pageData(listOf(episode(3 - page)), page, pages = 2) },
            onPage = { chapters, isLastPage ->
                collected += chapters
                snapshots += ChapterCatalog(collected.sortedBy { it.order }, isLastPage)
            },
            onPageError = { _, error -> throw error },
        )

        assertEquals(listOf(false, true), snapshots.map { it.isComplete })
        assertEquals(listOf(2), snapshots.first().chapters.map { it.order })
        assertEquals(listOf(1, 2), snapshots.last().chapters.map { it.order })
    }

    @Test
    fun `a failed later page preserves an incomplete catalog`() = runTest {
        val expectedError = IOException("Second page failed")
        val collected = mutableListOf<Chapter>()
        val snapshots = mutableListOf<ChapterCatalog>()
        val errors = mutableListOf<Pair<Int, Exception>>()
        walkEpisodePages(
            fetchPage = { page ->
                if (page == 2) throw expectedError
                pageData(listOf(episode(3)), page, pages = 3)
            },
            onPage = { chapters, isLastPage ->
                collected += chapters
                snapshots += ChapterCatalog(collected.sortedBy { it.order }, isLastPage)
            },
            onPageError = { page, error -> errors += page to error },
        )

        assertEquals(listOf(3), snapshots.single().chapters.map { it.order })
        assertFalse(snapshots.single().isComplete)
        assertEquals(2, errors.single().first)
        assertSame(expectedError, errors.single().second)
    }

    @Test
    fun `a catalog ending at the page limit is complete`() = runTest {
        val requests = mutableListOf<Int>()
        val completionFlags = mutableListOf<Boolean>()
        val collected = mutableListOf<Chapter>()
        walkEpisodePages(
            fetchPage = { page ->
                requests += page
                pageData(listOf(episode(page)), page, pages = MAX_EPISODE_FETCH_PAGES)
            },
            onPage = { chapters, isLastPage ->
                collected += chapters
                completionFlags += isLastPage
            },
            onPageError = { _, error -> throw error },
        )

        assertEquals((1..MAX_EPISODE_FETCH_PAGES).toList(), requests)
        assertEquals(MAX_EPISODE_FETCH_PAGES, collected.size)
        assertTrue(completionFlags.last())
        assertTrue(completionFlags.dropLast(1).none { it })
    }

    @Test
    fun `reaching the page limit preserves partial data without marking it complete`() = runTest {
        val requests = mutableListOf<Int>()
        val collected = mutableListOf<Chapter>()
        val completionFlags = mutableListOf<Boolean>()
        val errors = mutableListOf<Pair<Int, Exception>>()
        walkEpisodePages(
            fetchPage = { page ->
                requests += page
                pageData(listOf(episode(page)), page, pages = MAX_EPISODE_FETCH_PAGES + 1)
            },
            onPage = { chapters, isLastPage ->
                collected += chapters
                completionFlags += isLastPage
            },
            onPageError = { page, error -> errors += page to error },
        )

        assertEquals((1..MAX_EPISODE_FETCH_PAGES).toList(), requests)
        assertEquals(MAX_EPISODE_FETCH_PAGES, collected.size)
        assertTrue(completionFlags.none { it })
        assertEquals(MAX_EPISODE_FETCH_PAGES + 1, errors.single().first)
        assertIs<IOException>(errors.single().second)
    }

    @Test
    fun `full catalog loading rejects an incomplete result at the page limit`() = runTest {
        val requests = mutableListOf<Int>()
        assertFailsWith<IOException> {
            loadAllChapters { page ->
                requests += page
                pageData(listOf(episode(page)), page, pages = MAX_EPISODE_FETCH_PAGES + 1)
            }
        }

        assertEquals((1..MAX_EPISODE_FETCH_PAGES).toList(), requests)
    }

    private fun episode(order: Int) =
        Episode(id = "id_$order", title = "第${order}话", order = order, updatedAt = "")

    private fun pageData(docs: List<Episode>, page: Int, pages: Int) = PageData(
        total = pages,
        limit = 1,
        page = page,
        pages = pages,
        docs = docs,
    )
}
