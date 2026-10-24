package com.shizq.bika.core.data.paging

import androidx.paging.PagingSource
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.EpisodeData
import com.shizq.bika.core.network.model.PageData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChapterListPagingSourceTest {
    @Test
    fun `descending pages are merged into a globally ascending catalog`() = runTest {
        for (pageSize in listOf(40, 20)) {
            val pages = (56 downTo 1).map { episode(it) }.chunked(pageSize)
            val requests = mutableListOf<Int>()
            client { page ->
                requests += page
                pageData(pages[page - 1], page, pages.size)
            }.use { client ->
                val result = loadPage(ChapterListPagingSource("comic", BikaDataSource(client)))

                assertEquals((1..56).toList(), result.data.map { it.order })
                assertEquals((1..56).map { "id_$it" }, result.data.map { it.id })
                assertEquals((1..pages.size).toList(), requests)
                assertNull(result.prevKey)
                assertNull(result.nextKey)
            }
        }
    }

    @Test
    fun `empty single and page boundary catalogs finish without extra requests`() = runTest {
        for (size in listOf(0, 1, 40, 41)) {
            val pages = (1..size).map { episode(it) }.chunked(40)
                .ifEmpty { listOf(emptyList()) }
            val requests = mutableListOf<Int>()
            client { page ->
                requests += page
                pageData(pages[page - 1], page, pages.size)
            }.use { client ->
                val result = loadPage(ChapterListPagingSource("comic", BikaDataSource(client)))

                assertEquals((1..size).toList(), result.data.map { it.order })
                assertEquals((1..pages.size).toList(), requests)
                assertNull(result.nextKey)
            }
        }
    }

    @Test
    fun `duplicate ids are removed without stopping at an entirely duplicate page`() = runTest {
        val original = episode(3, title = "原始标题")
        val pages = listOf(
            listOf(episode(4), original, original),
            listOf(original.copy(title = "重复标题"), episode(4)),
            listOf(episode(2), episode(1)),
        )
        val requests = mutableListOf<Int>()
        client { page ->
            requests += page
            pageData(pages[page - 1], page, pages.size)
        }.use { client ->
            val result = loadPage(ChapterListPagingSource("comic", BikaDataSource(client)))

            assertEquals(listOf(1, 2, 3, 4), result.data.map { it.order })
            assertEquals("原始标题", result.data.single { it.id == original.id }.title)
            assertEquals(listOf(1, 2, 3), requests)
        }
    }

    @Test
    fun `empty response stops inflated page metadata using the requested page`() = runTest {
        val requests = mutableListOf<Int>()
        client { page ->
            requests += page
            val docs = when (page) {
                1 -> listOf(episode(5))
                2 -> listOf(episode(1))
                3 -> emptyList()
                else -> error("Unexpected request $page")
            }
            pageData(docs, page = 1, pages = 99)
        }.use { client ->
            val result = loadPage(ChapterListPagingSource("comic", BikaDataSource(client)))

            assertEquals(listOf(1, 5), result.data.map { it.order })
            assertEquals(listOf(1, 2, 3), requests)
            assertNull(result.nextKey)
        }
    }

    @Test
    fun `sorting preserves special chapters and original navigation orders`() = runTest {
        val pages = listOf(
            listOf(episode(80, title = "番外"), episode(40, title = "第2话")),
            listOf(
                episode(10, title = "第1话"),
                episode(30, title = "休刊通知"),
                episode(20, title = "第10话"),
                episode(30, id = "other_notice", title = "休刊通知"),
            ),
        )
        client { page -> pageData(pages[page - 1], page, pages.size) }.use { client ->
            val result = loadPage(ChapterListPagingSource("comic", BikaDataSource(client)))

            assertEquals(listOf(10, 20, 30, 30, 40, 80), result.data.map { it.order })
            assertEquals(
                listOf("第1话", "第10话", "休刊通知", "休刊通知", "第2话", "番外"),
                result.data.map { it.title },
            )
            assertEquals(
                listOf("id_10", "id_20", "id_30", "other_notice", "id_40", "id_80"),
                result.data.map { it.id },
            )
        }
    }

    @Test
    fun `later page failure returns an error and retry reloads a complete catalog`() = runTest {
        var failSecondPage = true
        val requests = mutableListOf<Int>()
        client { page ->
            requests += page
            if (page == 2 && failSecondPage) throw IOException("Temporary failure")
            pageData(listOf(episode(if (page == 1) 2 else 1)), page, pages = 2)
        }.use { client ->
            val source = ChapterListPagingSource("comic", BikaDataSource(client))
            assertIs<PagingSource.LoadResult.Error<Int, Chapter>>(source.load(refresh()))

            failSecondPage = false
            val result = loadPage(source)
            assertEquals(listOf(1, 2), result.data.map { it.order })
            assertEquals(listOf(1, 2, 1, 2), requests)

            assertEquals(result.data, loadPage(source).data)
            assertEquals(listOf(1, 2, 1, 2, 1, 2), requests)
        }
    }

    @Test
    fun `cancelled requests are propagated instead of becoming load errors`() = runTest {
        client { throw CancellationException("Cancelled") }.use { client ->
            val source = ChapterListPagingSource("comic", BikaDataSource(client))
            assertFailsWith<CancellationException> { source.load(refresh()) }
        }
    }

    @Test
    fun `cancelling an active load stops further page requests`() = runTest {
        val requests = mutableListOf<Int>()
        val secondPageStarted = CompletableDeferred<Unit>()
        client { page ->
            requests += page
            if (page == 2) {
                secondPageStarted.complete(Unit)
                awaitCancellation()
            }
            pageData(listOf(episode(3)), page, pages = 3)
        }.use { client ->
            val source = ChapterListPagingSource("comic", BikaDataSource(client))
            val loading = async { source.load(refresh()) }

            secondPageStarted.await()
            loading.cancelAndJoin()

            assertFailsWith<CancellationException> { loading.await() }
            assertEquals(listOf(1, 2), requests)
        }
    }

    private fun refresh() = PagingSource.LoadParams.Refresh<Int>(
        key = null,
        loadSize = 20,
        placeholdersEnabled = false,
    )

    private suspend fun loadPage(source: ChapterListPagingSource) =
        assertIs<PagingSource.LoadResult.Page<Int, Chapter>>(source.load(refresh()))

    private fun client(fetchPage: suspend (Int) -> PageData<Episode>) = HttpClient(MockEngine { request ->
        assertEquals("/comics/comic/eps", request.url.encodedPath)
        val page = requireNotNull(request.url.parameters["page"]).toInt()
        respond(
            content = Json.encodeToString(EpisodeData(fetchPage(page))),
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }) {
        defaultRequest { url("https://example.test/") }
        install(ContentNegotiation) { json() }
    }

    private fun episode(order: Int, id: String = "id_$order", title: String = "第${order}话") =
        Episode(id = id, title = title, order = order, updatedAt = "2026-10-24T00:00:00Z")

    private fun pageData(docs: List<Episode>, page: Int, pages: Int) = PageData(
        total = pages * 40,
        limit = 40,
        page = page,
        pages = pages,
        docs = docs,
    )
}
