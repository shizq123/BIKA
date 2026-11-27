package com.shizq.bika.ui.feed

import androidx.datastore.core.DataStore
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.domain.filter.ReadingProgressFilter
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
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class FeedReadingStatusFilterTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `阅读状态筛选支持单选多选清空以及历史库实时刷新，未开启时不触发重流`() = runTest(dispatcher) {
        val historiesFlow = MutableStateFlow(
            listOf(
                history("comic-updates", chapterId = 3, currentPage = 20, pageCount = 20),
                history("comic-completed", chapterId = 5, currentPage = 20, pageCount = 20),
                history("comic-read", chapterId = 5, currentPage = 2, pageCount = 20),
            )
        )
        val harness = Harness(this, InMemoryPreferences(), historiesFlow)
        try {
            runCurrent()
            assertEquals(
                listOf("comic-updates", "comic-completed", "comic-read", "comic-unread"),
                harness.ids,
            )
            val initialEmissionCount = harness.comicsEmissions

            // 未启用阅读状态筛选时，本地历史变更不应触发 Paging 流重发
            historiesFlow.value = historiesFlow.value + history(
                "comic-unread",
                chapterId = 1,
                currentPage = 1,
                pageCount = 20,
            )
            runCurrent()
            assertEquals(initialEmissionCount, harness.comicsEmissions)

            // 还原 comic-unread 为未读
            historiesFlow.value = historiesFlow.value.filterNot { it.history.id == "comic-unread" }
            runCurrent()

            // 只看已读完
            harness.vm.dispatch(FeedAction.ToggleReadingStatus(ReadingProgressFilter.Completed))
            runCurrent()
            assertEquals(listOf("comic-completed"), harness.ids)

            // 多选：已读完 + 有更新 (OR)
            harness.vm.dispatch(FeedAction.ToggleReadingStatus(ReadingProgressFilter.HasUpdates))
            runCurrent()
            assertEquals(listOf("comic-updates", "comic-completed"), harness.ids)

            // 切换到仅看未读
            harness.vm.dispatch(FeedAction.ClearFilters)
            harness.vm.dispatch(FeedAction.ToggleReadingStatus(ReadingProgressFilter.Unread))
            runCurrent()
            assertEquals(listOf("comic-unread"), harness.ids)

            // 开启阅读状态筛选后，详情页将 comic-unread 读成已读完返回，列表实时刷新将其移除
            historiesFlow.value = historiesFlow.value + history(
                "comic-unread",
                chapterId = 5,
                currentPage = 20,
                pageCount = 20,
            )
            runCurrent()
            assertEquals(emptyList(), harness.ids)

            // 清除筛选恢复全部
            harness.vm.dispatch(FeedAction.ClearFilters)
            runCurrent()
            assertEquals(
                listOf("comic-updates", "comic-completed", "comic-read", "comic-unread"),
                harness.ids,
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun `先选阅读状态再开启全局开关会持久化并跨列表生效`() = runTest(dispatcher) {
        val dataStore = InMemoryPreferences()
        val historiesFlow = MutableStateFlow(
            listOf(
                history("comic-updates", chapterId = 3, currentPage = 20, pageCount = 20),
                history("comic-completed", chapterId = 5, currentPage = 20, pageCount = 20),
                history("comic-read", chapterId = 5, currentPage = 2, pageCount = 20),
            )
        )
        val first = Harness(this, dataStore, historiesFlow)
        try {
            runCurrent()

            // 1. 先选局部「已读完」
            first.vm.dispatch(FeedAction.ToggleReadingStatus(ReadingProgressFilter.Completed))
            runCurrent()
            assertEquals(listOf("comic-completed"), first.ids)
            assertFalse(first.vm.uiState.value.readingStatusGlobal)

            // 2. 开启全局开关：自动升级「Completed」到全局，并清空局部 readingStatuses
            first.vm.dispatch(FeedAction.SetGlobalReadingStatusFilter(true))
            runCurrent()
            assertTrue(first.vm.uiState.value.readingStatusGlobal)
            assertEquals(setOf("Completed"), dataStore.current.filter.globalReadingStatuses)
            assertTrue(first.vm.uiState.value.query.criteria.readingStatuses.isEmpty())
            assertEquals(listOf("comic-completed"), first.ids)

            // 3. 全局开启状态下再选「未读」直接持久化并生效
            first.vm.dispatch(FeedAction.ToggleReadingStatus(ReadingProgressFilter.Unread))
            runCurrent()
            assertEquals(setOf("Completed", "Unread"), dataStore.current.filter.globalReadingStatuses)
            assertEquals(listOf("comic-completed", "comic-unread"), first.ids)

            // 4. 新建第二个列表页自动继承全局阅读状态筛选；清除局部筛选不影响全局
            val second = Harness(this, dataStore, historiesFlow)
            try {
                runCurrent()
                assertTrue(second.vm.uiState.value.readingStatusGlobal)
                assertEquals(listOf("comic-completed", "comic-unread"), second.ids)

                second.vm.dispatch(FeedAction.ToggleIncludedTopic("不存在"))
                runCurrent()
                second.vm.dispatch(FeedAction.ClearFilters)
                runCurrent()
                assertEquals(listOf("comic-completed", "comic-unread"), second.ids)
                assertEquals(setOf("Completed", "Unread"), dataStore.current.filter.globalReadingStatuses)
            } finally {
                second.close()
            }

            // 5. 关闭全局开关：停止过滤但保留历史；重开直接恢复
            first.vm.dispatch(FeedAction.SetGlobalReadingStatusFilter(false))
            runCurrent()
            assertFalse(first.vm.uiState.value.readingStatusGlobal)
            assertEquals(
                listOf("comic-updates", "comic-completed", "comic-read", "comic-unread"),
                first.ids,
            )
            assertEquals(setOf("Completed", "Unread"), dataStore.current.filter.globalReadingStatuses)

            first.vm.dispatch(FeedAction.SetGlobalReadingStatusFilter(true))
            runCurrent()
            assertEquals(listOf("comic-completed", "comic-unread"), first.ids)
        } finally {
            first.close()
        }
    }

    private inner class Harness(
        scope: TestScope,
        dataStore: DataStore<UserPreferences>,
        historiesFlow: Flow<List<DetailedHistory>>,
    ) {
        var comicsEmissions = 0
            private set

        val presenter = object : PagingDataPresenter<ComicSummary>(dispatcher) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<ComicSummary>) = Unit
        }

        private val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = this@FeedReadingStatusFilterTest.dispatcher
            requestHandlers.add {
                val payload = Json.encodeToString(
                    ComicResource(
                        PageData(
                            total = 4,
                            limit = 20,
                            page = 1,
                            pages = 1,
                            docs = listOf(
                                comic("comic-updates"),
                                comic("comic-completed"),
                                comic("comic-read"),
                                comic("comic-unread"),
                            ),
                        )
                    )
                )
                respond(payload, headers = headersOf(HttpHeaders.ContentType, "application/json"))
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
            userPreferencesDataSource = UserPreferencesDataSource(dataStore),
            historyDao = FakeReadingHistoryDao(historiesFlow),
            api = api,
            channelPagingSourceFactory = object : ChannelPagingSource.Factory {
                override fun create(channel: String, sort: SortOrder) =
                    ChannelPagingSource(api, channel, sort)
            },
            knightPagingSourceFactory = object : KnightPagingSource.Factory {
                override fun create(knightId: String, sort: SortOrder) =
                    KnightPagingSource(api, knightId, sort)
            },
            advancedSearchPagingSourceFactory = object : AdvancedSearchPagingSource.Factory {
                override fun create(query: String, sort: SortOrder) =
                    AdvancedSearchPagingSource(api, query, sort)
            },
            favouriteComicsPagingSourceFactory = object : FavouriteComicsPagingSource.Factory {
                override fun create(sort: SortOrder) = FavouriteComicsPagingSource(api, sort)
            },
            recentUpdatesPagingSourceProvider = Provider { RecentUpdatesPagingSource(api) },
            currentAction = DiscoveryAction.Channel("synthetic"),
        )

        private val collectors = listOf(
            scope.backgroundScope.launch(dispatcher) { vm.uiState.collect() },
            scope.backgroundScope.launch(dispatcher) {
                vm.pagingSession.filterNotNull().collectLatest { session ->
                    session.comics.collectLatest {
                        comicsEmissions++
                        presenter.collectFrom(it)
                    }
                }
            },
        )

        val ids: List<String>
            get() = presenter.snapshot().items.map { it.id }

        fun close() {
            collectors.forEach { it.cancel() }
            vm.viewModelScope.cancel()
            client.close()
            engine.close()
        }
    }

    private class InMemoryPreferences : DataStore<UserPreferences> {
        private val state = MutableStateFlow(UserPreferences())
        val current: UserPreferences get() = state.value
        override val data: Flow<UserPreferences> = state
        override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
            state.updateAndGet { transform(it) }
    }

    private class FakeReadingHistoryDao(
        private val historiesFlow: Flow<List<DetailedHistory>>,
    ) : ReadingHistoryDao {
        override suspend fun upsertHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun upsertHistories(histories: List<ReadingHistoryEntity>) = Unit
        override suspend fun deleteHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun deleteHistoryById(id: String) = Unit
        override suspend fun clearAllHistories() = Unit
        override suspend fun upsertChapterProgress(progress: ChapterProgressEntity) = Unit
        override suspend fun upsertChapterProgressList(progressList: List<ChapterProgressEntity>) = Unit
        override fun getDetailedHistories(): Flow<List<DetailedHistory>> = historiesFlow
        override fun getLatestDetailedHistory(): Flow<DetailedHistory?> = flowOf(null)
        override suspend fun getDetailedHistoryById(id: String): DetailedHistory? = null
        override suspend fun updateLastReadAt(id: String, newTimestamp: Instant): Int = 0
        override suspend fun updateIsFavourited(id: String, isFavourited: Boolean): Int = 0
        override fun getChapterProgressByComic(comicId: String): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
        override fun getAllChapterProgress(): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
    }

    private fun comic(id: String) = ComicSummary(
        id = id,
        title = "Title $id",
        author = "Author",
        epsCount = 5,
        image = RemoteImage(),
    )

    private fun history(
        id: String,
        chapterId: Int,
        currentPage: Int,
        pageCount: Int,
    ): DetailedHistory {
        val now = Instant.fromEpochMilliseconds(1000L)
        return DetailedHistory(
            history = ReadingHistoryEntity(
                id = id,
                title = "Title $id",
                author = "Author",
                coverUrl = "",
                lastInteractionAt = now,
                epsCount = 5,
            ),
            progressList = listOf(
                ChapterProgressEntity(
                    historyId = id,
                    chapterId = chapterId,
                    currentPage = currentPage,
                    pageCount = pageCount,
                    lastReadAt = now,
                )
            ),
        )
    }
}
