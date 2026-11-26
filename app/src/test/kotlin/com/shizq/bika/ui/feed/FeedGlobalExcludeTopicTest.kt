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
class FeedGlobalExcludeTopicTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `先选主题再开启全局开关会持久化并跨列表生效`() = runTest(dispatcher) {
        val dataStore = InMemoryPreferences()
        val first = Harness(this, dataStore)
        try {
            runCurrent()
            assertEquals(listOf("c1", "c2", "c3"), first.ids)

            // 1. 全局未开启时勾选局部排除主题「NTR」
            first.vm.dispatch(FeedAction.ToggleExcludedTopic("NTR"))
            runCurrent()
            assertEquals(listOf("c1", "c3"), first.ids)
            assertFalse(first.vm.uiState.value.excludeTopicsGlobal)

            // 2. 开启全局开关：自动升级「NTR」到全局存储，并清空局部 excludedTopics
            first.vm.dispatch(FeedAction.SetGlobalTopicFilter(true))
            runCurrent()
            assertTrue(first.vm.uiState.value.excludeTopicsGlobal)
            assertEquals(listOf("NTR"), dataStore.current.filter.globalBlockedTopics)
            assertTrue(first.vm.uiState.value.query.criteria.excludedTopics.isEmpty())
            assertEquals(listOf("c1", "c3"), first.ids)

            // 3. 全局开启状态下再勾选「重口地帶」直接持久化并立即生效
            first.vm.dispatch(FeedAction.ToggleExcludedTopic("重口地帶"))
            runCurrent()
            assertEquals(listOf("NTR", "重口地帶"), dataStore.current.filter.globalBlockedTopics)
            assertEquals(listOf("c1"), first.ids)

            // 4. 新建另一个频道页面（共享同一份持久化偏好），自动继承全局排除主题
            val second = Harness(this, dataStore)
            try {
                runCurrent()
                assertTrue(second.vm.uiState.value.excludeTopicsGlobal)
                assertEquals(setOf("NTR", "重口地帶"), second.vm.uiState.value.data.globalPolicy.blockedTopics)
                assertEquals(listOf("c1"), second.ids)

                // 清除当前页面局部筛选不应清掉全局偏好
                second.vm.dispatch(FeedAction.ToggleIncludedTopic("純愛"))
                runCurrent()
                second.vm.dispatch(FeedAction.ClearFilters)
                runCurrent()
                assertEquals(listOf("c1"), second.ids)
                assertEquals(listOf("NTR", "重口地帶"), dataStore.current.filter.globalBlockedTopics)
            } finally {
                second.close()
            }

            // 5. 关闭全局开关：停止过滤但保留历史列表；重开后直接恢复
            first.vm.dispatch(FeedAction.SetGlobalTopicFilter(false))
            runCurrent()
            assertFalse(first.vm.uiState.value.excludeTopicsGlobal)
            assertEquals(listOf("c1", "c2", "c3"), first.ids)
            assertEquals(listOf("NTR", "重口地帶"), dataStore.current.filter.globalBlockedTopics)

            first.vm.dispatch(FeedAction.SetGlobalTopicFilter(true))
            runCurrent()
            assertEquals(listOf("c1"), first.ids)

            // 6. 在全局开启状态下取消勾选「NTR」会从全局持久化移除
            first.vm.dispatch(FeedAction.ToggleExcludedTopic("NTR"))
            runCurrent()
            assertEquals(listOf("重口地帶"), dataStore.current.filter.globalBlockedTopics)
            assertEquals(listOf("c1", "c2"), first.ids)
        } finally {
            first.close()
        }
    }

    private inner class Harness(
        scope: TestScope,
        dataStore: DataStore<UserPreferences>,
    ) {
        val presenter = object : PagingDataPresenter<ComicSummary>(dispatcher) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<ComicSummary>) = Unit
        }

        private val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = this@FeedGlobalExcludeTopicTest.dispatcher
            requestHandlers.add {
                val payload = Json.encodeToString(
                    ComicResource(
                        PageData(
                            total = 3,
                            limit = 20,
                            page = 1,
                            pages = 1,
                            docs = listOf(
                                comic("c1", categories = listOf("純愛")),
                                comic("c2", categories = listOf("NTR")),
                                comic("c3", categories = listOf("重口地帶")),
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
            historyDao = EmptyReadingHistoryDao,
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
                    session.comics.collectLatest { presenter.collectFrom(it) }
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

    private object EmptyReadingHistoryDao : ReadingHistoryDao {
        override suspend fun upsertHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun upsertHistories(histories: List<ReadingHistoryEntity>) = Unit
        override suspend fun deleteHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun deleteHistoryById(id: String) = Unit
        override suspend fun clearAllHistories() = Unit
        override suspend fun upsertChapterProgress(progress: ChapterProgressEntity) = Unit
        override suspend fun upsertChapterProgressList(progressList: List<ChapterProgressEntity>) = Unit
        override fun getDetailedHistories(): Flow<List<DetailedHistory>> = flowOf(emptyList())
        override fun getLatestDetailedHistory(): Flow<DetailedHistory?> = flowOf(null)
        override suspend fun getDetailedHistoryById(id: String): DetailedHistory? = null
        override suspend fun updateLastReadAt(id: String, newTimestamp: Instant): Int = 0
        override suspend fun updateIsFavourited(id: String, isFavourited: Boolean): Int = 0
        override fun getChapterProgressByComic(comicId: String): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
        override fun getAllChapterProgress(): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
    }

    private fun comic(id: String, categories: List<String>) = ComicSummary(
        id = id,
        title = "Title $id",
        author = "Author",
        categories = categories,
        image = RemoteImage(),
    )
}
