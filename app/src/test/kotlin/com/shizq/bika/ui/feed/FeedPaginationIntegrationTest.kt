package com.shizq.bika.ui.feed

import androidx.datastore.core.DataStore
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.ComicResource
import com.shizq.bika.core.network.model.PageData
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.KnightPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Provider
import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Paging's Android artifact calls android.util.Log; use the existing JVM Robolectric dependency.
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class FeedPaginationIntegrationTest(private val feed: String) {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `等待关闭的初始偏好再加载一页且消费尾项不追加`() = runTest(dispatcher) {
        val firstRead = CompletableDeferred<Unit>()
        Fixture(this, firstRead = firstRead).use { f ->
            runCurrent()
            assertFalse(f.vm.uiState.value.preferencesReady)
            assertTrue(f.sessions.isEmpty())
            assertTrue(f.sources.isEmpty())
            f.expectRequests()

            firstRead.complete(Unit)
            runCurrent()
            f.expectPage(1)
            assertTrue(f.vm.uiState.value.preferencesReady)
            assertFalse(f.sessions.single().continuousScrollEnabled)
            f.consumeTail()
            runCurrent()
            f.expectPage(1)
            f.expectRequests(1)
            assertEquals(1, f.sources.size)
        }
    }

    @Test
    fun `下一页和上一页替换当前列表且首页不能继续后退`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.expectPage(1)
            f.vm.dispatch(FeedAction.PreviousPage)
            runCurrent()
            f.expectRequests(1)

            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            f.expectPage(2)
            assertTrue(f.pagination.hasPrevious)
            f.consumeTail()
            runCurrent()
            f.expectRequests(1, 2)

            f.vm.dispatch(FeedAction.PreviousPage)
            runCurrent()
            f.expectPage(1)
            assertFalse(f.pagination.hasPrevious)
            f.expectRequests(1, 2, 1)
        }
    }

    @Test
    fun `手动输入直达末页替换列表且尾项不追加刷新仍保留末页并能跳回首页`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.expectPage(1)
            assertTrue(f.pagination.isManual)
            f.expectRequests(1)

            f.vm.dispatch(FeedAction.JumpToPage(TOTAL_PAGES))
            runCurrent()
            f.expectPage(TOTAL_PAGES)
            assertFalse(f.pagination.hasNext)
            f.expectRequests(1, 3)

            f.consumeTail()
            runCurrent()
            f.expectPage(TOTAL_PAGES)
            f.expectRequests(1, 3)

            val oldSource = f.sources.last()
            f.presenter.refresh()
            runCurrent()
            f.expectPage(TOTAL_PAGES)
            f.expectRequests(1, 3, 3)
            assertNotSame(oldSource, f.sources.last())
            assertTrue(oldSource.invalid)

            f.vm.dispatch(FeedAction.JumpToPage(1))
            runCurrent()
            f.expectPage(1)
            assertFalse(f.pagination.hasPrevious)
            f.expectRequests(1, 3, 3, 1)
        }
    }

    @Test
    fun `手动输入越界或当前页不加载且自动模式拒绝跳页`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.expectPage(1)
            f.vm.dispatch(FeedAction.JumpToPage(2))
            runCurrent()
            f.expectPage(2)
            assertTrue(f.pagination.isManual)
            f.expectRequests(1, 2)
            val manualPagination = f.pagination
            val manualSource = f.sources.last()

            for (page in listOf(0, -1, TOTAL_PAGES + 1, Int.MAX_VALUE, 2)) {
                f.vm.dispatch(FeedAction.JumpToPage(page))
                runCurrent()
                f.expectPage(2)
                f.expectRequests(1, 2)
                assertEquals(manualPagination, f.pagination, "JumpToPage($page) must be ignored")
                assertSame(manualSource, f.sources.last())
            }

            f.preferences.setContinuousScrollEnabled(true)
            runCurrent()
            f.expectPage(1)
            assertTrue(f.pagination.continuousScrollEnabled)
            f.expectRequests(1, 2, 1)
            val automaticPagination = f.pagination
            val automaticSource = f.sources.last()

            f.vm.dispatch(FeedAction.JumpToPage(TOTAL_PAGES))
            runCurrent()
            f.expectPage(1)
            f.expectRequests(1, 2, 1)
            assertEquals(automaticPagination, f.pagination)
            assertSame(automaticSource, f.sources.last())
        }
    }

    @Test
    fun `末页刷新创建新source并仍然请求末页`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            repeat(TOTAL_PAGES - 1) {
                f.vm.dispatch(FeedAction.NextPage)
                runCurrent()
            }
            f.expectPage(TOTAL_PAGES)
            assertFalse(f.pagination.hasNext)
            f.consumeTail() // Establish a refresh anchor on the final page.
            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            f.expectRequests(1, 2, 3)

            val oldSource = f.sources.last()
            f.presenter.refresh()
            runCurrent()
            f.expectPage(TOTAL_PAGES)
            f.expectRequests(1, 2, 3, 3)
            assertEquals(4, f.sources.size)
            assertNotSame(oldSource, f.sources.last())
            assertTrue(oldSource.invalid)
        }
    }

    @Test
    fun `实时开启恢复尾项追加而关闭重置第一页`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            f.expectPage(2)

            f.preferences.setContinuousScrollEnabled(true)
            runCurrent()
            f.expectPage(1)
            assertTrue(f.pagination.continuousScrollEnabled)
            f.consumeTail()
            runCurrent()
            assertEquals(pageIds(1) + pageIds(2), f.ids)
            f.expectRequests(1, 2, 1, 2)
            assertEquals(3, f.sources.size) // Append reuses the current real source.

            f.preferences.setContinuousScrollEnabled(false)
            runCurrent()
            f.expectPage(1)
            assertTrue(f.pagination.isManual)
            f.consumeTail()
            runCurrent()
            f.expectRequests(1, 2, 1, 2, 1)
            assertEquals(4, f.sources.size)
        }
    }

    @Test
    fun `本地筛选复用已加载页且筛空当前页仍保留服务端总页数`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            f.expectPage(2)
            val source = f.sources.last()
            val revision = f.pagination.revision

            f.vm.dispatch(FeedAction.ToggleIncludedTopic("kept"))
            runCurrent()
            assertEquals(pageIds(2).filterIndexed { index, _ -> index % 2 == 0 }, f.ids)
            f.expectRequests(1, 2)
            f.vm.dispatch(FeedAction.ToggleExcludedTopic("kept"))
            runCurrent()
            assertTrue(f.ids.isEmpty())
            assertEquals(2, f.pagination.currentPage)
            assertEquals(TOTAL_PAGES, f.pagination.totalPages)
            assertTrue(f.pagination.hasNext)
            assertTrue(f.pagination.hasPrevious)
            f.expectRequests(1, 2)

            f.vm.dispatch(FeedAction.ClearFilters)
            runCurrent()
            f.expectPage(2)
            f.expectRequests(1, 2)
            assertEquals(revision, f.pagination.revision)
            assertEquals(2, f.sources.size)
            assertSame(source, f.sources.last())
        }
    }

    @Test
    fun `改变排序回到第一页且重复选择同排序不重新加载`() = runTest(dispatcher) {
        Fixture(this).use { f ->
            runCurrent()
            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            f.expectPage(2)
            val source = f.sources.last()

            f.vm.dispatch(FeedAction.ChangeSort(SortOrder.OLDEST))
            runCurrent()
            f.expectPage(1, SortOrder.OLDEST)
            f.expectRequests(1, 2, 1)
            assertEquals(SortOrder.OLDEST, f.sessions.last().sort)
            // RecentUpdates intentionally always sends NEWEST to the API.
            assertEquals(wireSort(SortOrder.OLDEST), f.requests.last().sort)
            assertNotSame(source, f.sources.last())

            f.vm.dispatch(FeedAction.ChangeSort(SortOrder.OLDEST))
            runCurrent()
            f.expectRequests(1, 2, 1)
            assertEquals(3, f.sources.size)
        }
    }

    @Test
    fun `翻页取消旧页在途请求且旧响应不能覆盖新页`() = runTest(dispatcher) {
        val pending = PendingSecondPage()
        Fixture(this, pending = pending).use { f ->
            runCurrent()
            f.expectPage(1)
            f.vm.dispatch(FeedAction.NextPage)
            runCurrent()
            assertTrue(pending.started.isCompleted)
            assertEquals(2, f.pagination.currentPage)
            f.expectRequests(1, 2)

            f.vm.dispatch(FeedAction.PreviousPage)
            runCurrent()
            assertTrue(pending.cancelled.isCompleted, "Changing page must cancel the old HTTP request")
            pending.release.complete(Unit)
            runCurrent()
            f.expectPage(1)
            f.expectRequests(1, 2, 1)
            assertEquals(listOf(1, 1), f.completedPages)
        }
    }

    @Test
    fun `关闭连续追加取消在途append并保持手动第一页`() = runTest(dispatcher) {
        val pending = PendingSecondPage()
        Fixture(this, continuous = true, pending = pending).use { f ->
            runCurrent()
            f.expectPage(1)
            f.consumeTail()
            runCurrent()
            assertTrue(pending.started.isCompleted)
            f.expectRequests(1, 2)

            f.preferences.setContinuousScrollEnabled(false)
            runCurrent()
            assertTrue(pending.cancelled.isCompleted, "Disabling append must cancel the old HTTP request")
            pending.release.complete(Unit)
            runCurrent()
            f.expectPage(1)
            assertTrue(f.pagination.isManual)
            f.consumeTail()
            runCurrent()
            f.expectRequests(1, 2, 1)
            assertEquals(listOf(1, 1), f.completedPages)
        }
    }

    private inner class Fixture(
        scope: TestScope,
        continuous: Boolean = false,
        firstRead: CompletableDeferred<Unit>? = null,
        pending: PendingSecondPage? = null,
    ) : Closeable {
        val preferences = UserPreferencesDataSource(InMemoryDataStore(continuous, firstRead))
        val requests = mutableListOf<Request>()
        val completedPages = mutableListOf<Int>()
        val sources = mutableListOf<PagingSource<Int, ComicSummary>>()
        val sessions = mutableListOf<FeedPagingRequest>()
        val presenter = object : PagingDataPresenter<ComicSummary>(dispatcher) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<ComicSummary>) = Unit
        }
        private val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = this@FeedPaginationIntegrationTest.dispatcher
            requestHandlers.add { request ->
                val params = request.url.parameters
                val page = requireNotNull(params["page"]).toInt()
                assertTrue(page in 1..TOTAL_PAGES)
                val expectedPath = when (feed) {
                    "AdvancedSearch" -> "/comics/advanced-search"
                    "Favourite" -> "/users/favourite"
                    else -> "/comics"
                }
                assertEquals(expectedPath, request.url.encodedPath)
                assertEquals(if (feed == "Channel") "synthetic-channel" else null, params["c"])
                assertEquals(if (feed == "Knight") "synthetic-knight-id" else null, params["ca"])
                val sort = if (feed == "AdvancedSearch") {
                    assertEquals(HttpMethod.Post, request.method)
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals("synthetic-query", body.getValue("keyword").jsonPrimitive.content)
                    body.getValue("sort").jsonPrimitive.content
                } else {
                    assertEquals(HttpMethod.Get, request.method)
                    requireNotNull(params["s"])
                }
                requests += Request(page, sort)
                pending?.hold(page)
                completedPages += page
                respond(response(page, sort), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        private val client = HttpClient(engine) {
            defaultRequest {
                url("https://feed.invalid/")
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
            }
        }
        private val api = BikaDataSource(client)
        val vm = FeedViewModel(
            userPreferencesDataSource = preferences,
            api = api,
            channelPagingSourceFactory = object : ChannelPagingSource.Factory {
                override fun create(channel: String, sort: SortOrder) =
                    track(ChannelPagingSource(api, channel, sort))
            },
            knightPagingSourceFactory = object : KnightPagingSource.Factory {
                override fun create(knightId: String, sort: SortOrder) =
                    track(KnightPagingSource(api, knightId, sort))
            },
            advancedSearchPagingSourceFactory = object : AdvancedSearchPagingSource.Factory {
                override fun create(query: String, sort: SortOrder) =
                    track(AdvancedSearchPagingSource(api, query, sort))
            },
            favouriteComicsPagingSourceFactory = object : FavouriteComicsPagingSource.Factory {
                override fun create(sort: SortOrder) = track(FavouriteComicsPagingSource(api, sort))
            },
            recentUpdatesPagingSourceProvider = Provider { track(RecentUpdatesPagingSource(api)) },
            currentAction = when (feed) {
                "Channel" -> DiscoveryAction.Channel("synthetic-channel")
                "AdvancedSearch" -> DiscoveryAction.AdvancedSearch("synthetic-query")
                "Knight" -> DiscoveryAction.Knight("synthetic-knight-name", "synthetic-knight-id")
                "Favourite" -> DiscoveryAction.ToFavourite
                "Recent" -> DiscoveryAction.ToRecent
                else -> error("Unknown feed: $feed")
            },
        )
        private val collectors = listOf(
            scope.backgroundScope.launch(dispatcher) { vm.uiState.collect() },
            scope.backgroundScope.launch(dispatcher) {
                vm.pagingSession.filterNotNull().collectLatest { session ->
                    sessions += session.request
                    session.comics.collectLatest { presenter.collectFrom(it) }
                }
            },
        )
        val pagination get() = vm.uiState.value.pagination
        val ids get() = presenter.snapshot().items.map { it.id }

        private fun <T : PagingSource<Int, ComicSummary>> track(source: T): T =
            source.also { sources += it }

        fun consumeTail() {
            assertTrue(presenter.size > 0)
            presenter[presenter.size - 1] // Unlike snapshot(), get() emits the real viewport hint.
        }

        fun expectPage(page: Int, sort: SortOrder = SortOrder.NEWEST) {
            assertEquals(pageIds(page, sort), ids)
            assertEquals(page, pagination.currentPage)
            assertEquals(TOTAL_PAGES, pagination.totalPages)
            val states = requireNotNull(presenter.loadStateFlow.value)
            assertTrue(states.refresh is LoadState.NotLoading, "$states")
            assertTrue(states.append is LoadState.NotLoading, "$states")
        }

        fun expectRequests(vararg pages: Int) = assertEquals(pages.toList(), requests.map { it.page })

        override fun close() {
            collectors.forEach { it.cancel() }
            vm.viewModelScope.cancel()
            client.close()
            engine.close()
        }
    }

    private class InMemoryDataStore(
        continuous: Boolean,
        firstRead: CompletableDeferred<Unit>?,
    ) : DataStore<UserPreferences> {
        private val state = MutableStateFlow(UserPreferences().let {
            it.copy(app = it.app.copy(continuousScrollEnabled = continuous))
        })
        private val mutex = Mutex()
        override val data: Flow<UserPreferences> = flow {
            firstRead?.await()
            emitAll(state)
        }

        override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }

    private class PendingSecondPage {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        suspend fun hold(page: Int) {
            if (page != 2 || started.isCompleted) return
            started.complete(Unit)
            try {
                release.await()
            } catch (e: CancellationException) {
                cancelled.complete(Unit)
                throw e
            }
        }
    }

    private data class Request(val page: Int, val sort: String)

    private fun wireSort(sort: SortOrder) =
        (if (feed == "Recent") SortOrder.NEWEST else sort).value

    private fun pageIds(page: Int, sort: SortOrder = SortOrder.NEWEST) =
        List(PAGE_SIZE) { "${wireSort(sort)}-p$page-$it" }

    private fun response(page: Int, sort: String) = Json.encodeToString(
        ComicResource(PageData(
            total = TOTAL_PAGES * PAGE_SIZE,
            limit = PAGE_SIZE,
            page = page,
            pages = TOTAL_PAGES,
            docs = List(PAGE_SIZE) { index ->
                ComicSummary(
                    id = "$sort-p$page-$index",
                    title = "Synthetic page $page item $index",
                    categories = listOf(if (index % 2 == 0) "kept" else "other"),
                    image = RemoteImage(),
                )
            },
        )),
    )

    companion object {
        private const val PAGE_SIZE = 20
        private const val TOTAL_PAGES = 3

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun feeds(): List<Array<Any>> =
            listOf("Channel", "AdvancedSearch", "Knight", "Favourite", "Recent").map { arrayOf<Any>(it) }
    }
}
