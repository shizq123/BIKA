package com.shizq.bika.ui.feed

import android.app.Application
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.datastore.core.DataStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
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
import com.shizq.bika.navigation.BottomSheetSceneStrategy
import com.shizq.bika.navigation.ConnectedRoute
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h800dp-xhdpi")
@OptIn(ExperimentalSharedTransitionApi::class)
class FeedScrollPositionRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `详情页新增屏蔽标签后返回列表保留原滚动位置`() {
        runFeedRoundTrip(
            continuous = false,
            nextPage = false,
            scrollTarget = 12,
            onInDetail = { preferences ->
                runBlocking { preferences.addBlockedTag("unrelated-blocked-tag") }
            },
        )
    }

    @Test
    fun `手动翻页第二页进详情返回后保留原滚动位置`() {
        runFeedRoundTrip(
            continuous = false,
            nextPage = true,
            scrollTarget = 12,
        )
    }

    @Test
    fun `连续滚动加载第二页进详情返回后保留原滚动位置`() {
        runFeedRoundTrip(
            continuous = true,
            nextPage = false,
            scrollTarget = 25,
        )
    }

    @Test
    fun `阅读后返回列表实时显示观看记录标签且保留原滚动位置`() {
        val historyDao = FakeReadingHistoryDao()
        runFeedRoundTrip(
            continuous = false,
            nextPage = false,
            scrollTarget = 12,
            historyDao = historyDao,
            onInDetail = {
                historyDao.histories.value = listOf(
                    DetailedHistory(
                        history = ReadingHistoryEntity(
                            id = "comic-p1-12",
                            title = "Comic p1-#12",
                            author = "Author",
                            coverUrl = "",
                            epsCount = 3,
                            lastInteractionAt = Instant.fromEpochMilliseconds(1_000L),
                            isFavourited = true,
                        ),
                        progressList = listOf(
                            ChapterProgressEntity(
                                historyId = "comic-p1-12",
                                chapterId = 3,
                                currentPage = 20,
                                pageCount = 20,
                                lastReadAt = Instant.fromEpochMilliseconds(1_000L),
                            ),
                        ),
                    ),
                )
            },
            onAfterReturn = {
                composeRule.waitUntil(5_000L) {
                    composeRule.onAllNodesWithText("已读完", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                assertTrue(
                    composeRule.onAllNodesWithText("已收藏", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                    "返回列表后应同步显示本地已收藏标签",
                )
            },
        )
    }

    @Test
    fun `返回首帧数据尚未到达时不被空态钳位待数据就绪后恢复原位置`() {
        val gate = CompletableDeferred<Unit>()
        var delaySecondSession = false
        val preferences = UserPreferencesDataSource(InMemoryDataStore(continuous = false))
        val historyDao = FakeReadingHistoryDao()
        val engine = MockEngine(MockEngineConfig().apply {
            requestHandlers.add { request ->
                if (delaySecondSession) {
                    gate.await()
                }
                val page = requireNotNull(request.url.parameters["page"]).toInt()
                respond(response(page), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        val client = createMockClient(engine)
        val api = BikaDataSource(client)
        var currentVm = createViewModel(preferences, historyDao, api, DiscoveryAction.Channel("test"))
        lateinit var backStack: NavBackStack<NavKey>

        try {
            composeRule.setContent {
                MaterialTheme {
                    backStack = rememberNavBackStack(
                        ConnectedRoute.DashboardRoute,
                        ConnectedRoute.FeedRoute(DiscoveryAction.Channel("test")),
                    )
                    SharedTransitionLayout {
                        NavDisplay(
                            backStack = backStack,
                            sceneStrategies = listOf(
                                BottomSheetSceneStrategy(),
                                DialogSceneStrategy(),
                            ),
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                                rememberResultEventBusNavEntryDecorator(),
                            ),
                            entryProvider = entryProvider {
                                entry<ConnectedRoute.DashboardRoute> { Box(Modifier.fillMaxSize()) }
                                entry<ConnectedRoute.FeedRoute>(metadata = noAnimMetadata()) { key ->
                                    FeedRoute(
                                        title = key.action.name,
                                        viewModel = currentVm,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                        onNavigate = { dest ->
                                            if (dest is FeedDestination.Back) backStack.removeLastOrNull()
                                        },
                                    )
                                }
                                entry<ConnectedRoute.UnitedDetailRoute>(metadata = noAnimMetadata()) {
                                    Box(Modifier.fillMaxSize()) { Text("Detail") }
                                }
                            },
                        )
                    }
                }
            }

            composeRule.waitUntil(5_000L) {
                hasListNode() && visibleComicTitles().any { it.startsWith("Comic p1-") }
            }
            composeRule.onNodeWithTag(FeedTestTags.List).performScrollToIndex(12)
            composeRule.waitUntil(5_000L) {
                visibleComicTitles().any { it == "Comic p1-#12" }
            }
            val beforeTitles = visibleComicTitles()
            assertTrue(beforeTitles.isNotEmpty(), "滚动前应可见漫画卡片")

            // 进入详情后模拟 ViewModel 重建且首屏网络请求尚未返回（复现首帧 itemCount == 0）
            composeRule.runOnIdle {
                backStack.add(ConnectedRoute.UnitedDetailRoute("comic-p1-12"))
            }
            composeRule.waitUntil(5_000L) { !hasListNode() }
            currentVm.viewModelScope.cancel()
            delaySecondSession = true
            currentVm = createViewModel(preferences, historyDao, api, DiscoveryAction.Channel("test"))

            // 返回列表：首帧数据尚未到达，展示加载覆盖层但 LazyColumn 保持 0 项不钳位位置。
            // 加载态包含无限旋转的 CircularProgressIndicator，需先关闭 autoAdvance 手动推进帧。
            composeRule.mainClock.autoAdvance = false
            composeRule.runOnUiThread {
                backStack.removeLastOrNull()
            }
            composeRule.waitUntil(5_000L) {
                composeRule.mainClock.advanceTimeByFrame()
                hasListNode()
            }
            repeat(4) {
                composeRule.mainClock.advanceTimeByFrame()
            }
            assertTrue(visibleComicTitles().isEmpty(), "网络放行前应处于加载状态")

            // 放行网络响应后恢复自动时钟，应精确恢复到离开前的列表位置
            gate.complete(Unit)
            composeRule.mainClock.autoAdvance = true
            composeRule.waitUntil(5_000L) {
                visibleComicTitles().isNotEmpty()
            }
            composeRule.waitForIdle()
            val afterTitles = visibleComicTitles()
            assertEquals(beforeTitles, afterTitles, "首帧空数据等待加载完成后应恢复原滚动位置")
        } finally {
            currentVm.viewModelScope.cancel()
            client.close()
            engine.close()
        }
    }

    private fun runFeedRoundTrip(
        continuous: Boolean,
        nextPage: Boolean,
        scrollTarget: Int,
        historyDao: FakeReadingHistoryDao = FakeReadingHistoryDao(),
        onInDetail: (UserPreferencesDataSource) -> Unit = {},
        onAfterReturn: () -> Unit = {},
    ) {
        val preferences = UserPreferencesDataSource(InMemoryDataStore(continuous = continuous))
        val engine = MockEngine(MockEngineConfig().apply {
            requestHandlers.add { request ->
                val page = requireNotNull(request.url.parameters["page"]).toInt()
                respond(response(page), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        val client = createMockClient(engine)
        val api = BikaDataSource(client)
        val action = DiscoveryAction.Channel("test")
        val vm = createViewModel(preferences, historyDao, api, action)
        lateinit var backStack: NavBackStack<NavKey>

        try {
            composeRule.setContent {
                MaterialTheme {
                    backStack = rememberNavBackStack(
                        ConnectedRoute.DashboardRoute,
                        ConnectedRoute.FeedRoute(action),
                    )
                    SharedTransitionLayout {
                        NavDisplay(
                            backStack = backStack,
                            sceneStrategies = listOf(
                                BottomSheetSceneStrategy(),
                                DialogSceneStrategy(),
                            ),
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                                rememberResultEventBusNavEntryDecorator(),
                            ),
                            entryProvider = entryProvider {
                                entry<ConnectedRoute.DashboardRoute> { Box(Modifier.fillMaxSize()) }
                                entry<ConnectedRoute.FeedRoute>(metadata = noAnimMetadata()) { key ->
                                    FeedRoute(
                                        title = key.action.name,
                                        viewModel = vm,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                        onNavigate = { dest ->
                                            if (dest is FeedDestination.Back) backStack.removeLastOrNull()
                                        },
                                    )
                                }
                                entry<ConnectedRoute.UnitedDetailRoute>(metadata = noAnimMetadata()) {
                                    Box(Modifier.fillMaxSize()) { Text("Detail") }
                                }
                            },
                        )
                    }
                }
            }

            composeRule.waitUntil(5_000L) {
                hasListNode() && visibleComicTitles().any { it.startsWith("Comic p1-") }
            }
            if (nextPage) {
                vm.dispatch(FeedAction.NextPage)
                composeRule.waitUntil(5_000L) {
                    hasListNode() && visibleComicTitles().any { it.startsWith("Comic p2-") }
                }
            }
            if (continuous && scrollTarget >= 20) {
                composeRule.onNodeWithTag(FeedTestTags.List).performScrollToIndex(18)
                composeRule.waitUntil(5_000L) {
                    visibleComicTitles().any { it.startsWith("Comic p1-#18") }
                }
                composeRule.waitForIdle()
            }
            composeRule.onNodeWithTag(FeedTestTags.List).performScrollToIndex(scrollTarget)
            val expectedPrefix = if (nextPage || (continuous && scrollTarget >= 20)) "Comic p2-" else "Comic p1-"
            composeRule.waitUntil(5_000L) {
                visibleComicTitles().any { it.startsWith(expectedPrefix) }
            }
            composeRule.waitForIdle()
            val beforeTitles = visibleComicTitles()
            assertTrue(beforeTitles.isNotEmpty(), "进入详情前应渲染目标位置的漫画卡片")

            composeRule.runOnIdle {
                backStack.add(ConnectedRoute.UnitedDetailRoute("comic-clicked"))
            }
            composeRule.waitUntil(5_000L) { !hasListNode() }
            onInDetail(preferences)
            composeRule.waitForIdle()

            composeRule.runOnIdle {
                backStack.removeLastOrNull()
            }
            composeRule.waitUntil(5_000L) {
                hasListNode() && visibleComicTitles().isNotEmpty()
            }
            composeRule.waitForIdle()
            val afterTitles = visibleComicTitles()
            assertEquals(beforeTitles, afterTitles, "从详情页返回后应恢复离开前的列表位置")
            onAfterReturn()
        } finally {
            vm.viewModelScope.cancel()
            client.close()
            engine.close()
        }
    }

    private fun noAnimMetadata() = metadata {
        put(NavDisplay.TransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
        put(NavDisplay.PopTransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
    }

    private fun hasListNode(): Boolean =
        composeRule.onAllNodesWithTag(FeedTestTags.List, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun visibleComicTitles(): List<String> =
        composeRule.onAllNodesWithText("Comic ", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") }
            .filter { it.startsWith("Comic ") }
            .distinct()

    private fun createMockClient(engine: MockEngine) = HttpClient(engine) {
        defaultRequest {
            url("https://feed.invalid/")
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
    }

    private fun createViewModel(
        preferences: UserPreferencesDataSource,
        historyDao: ReadingHistoryDao,
        api: BikaDataSource,
        action: DiscoveryAction,
    ) = FeedViewModel(
        userPreferencesDataSource = preferences,
        historyDao = historyDao,
        api = api,
        channelPagingSourceFactory = object : ChannelPagingSource.Factory {
            override fun create(channel: String, sort: SortOrder) = ChannelPagingSource(api, channel, sort)
        },
        knightPagingSourceFactory = object : KnightPagingSource.Factory {
            override fun create(knightId: String, sort: SortOrder) = KnightPagingSource(api, knightId, sort)
        },
        advancedSearchPagingSourceFactory = object : AdvancedSearchPagingSource.Factory {
            override fun create(query: String, sort: SortOrder) = AdvancedSearchPagingSource(api, query, sort)
        },
        favouriteComicsPagingSourceFactory = object : FavouriteComicsPagingSource.Factory {
            override fun create(sort: SortOrder) = FavouriteComicsPagingSource(api, sort)
        },
        recentUpdatesPagingSourceProvider = Provider { RecentUpdatesPagingSource(api) },
        currentAction = action,
    )

    private class InMemoryDataStore(continuous: Boolean) : DataStore<UserPreferences> {
        private val state = MutableStateFlow(UserPreferences().let {
            it.copy(app = it.app.copy(continuousScrollEnabled = continuous))
        })
        private val mutex = Mutex()
        override val data: Flow<UserPreferences> = state
        override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }

    private class FakeReadingHistoryDao : ReadingHistoryDao {
        val histories = MutableStateFlow<List<DetailedHistory>>(emptyList())
        override suspend fun upsertHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun upsertHistories(histories: List<ReadingHistoryEntity>) = Unit
        override suspend fun deleteHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun deleteHistoryById(id: String) = Unit
        override suspend fun clearAllHistories() = Unit
        override suspend fun upsertChapterProgress(progress: ChapterProgressEntity) = Unit
        override suspend fun upsertChapterProgressList(progressList: List<ChapterProgressEntity>) = Unit
        override fun getDetailedHistories(): Flow<List<DetailedHistory>> = histories
        override fun getLatestDetailedHistory(): Flow<DetailedHistory?> = flowOf(null)
        override suspend fun getDetailedHistoryById(id: String): DetailedHistory? = null
        override suspend fun updateLastReadAt(id: String, newTimestamp: Instant): Int = 0
        override suspend fun updateIsFavourited(id: String, isFavourited: Boolean): Int = 0
        override fun getChapterProgressByComic(comicId: String): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
        override fun getAllChapterProgress(): Flow<List<ChapterProgressEntity>> = flowOf(emptyList())
    }

    private fun response(page: Int) = Json.encodeToString(
        ComicResource(
            PageData(
                total = 60,
                limit = 20,
                page = page,
                pages = 3,
                docs = List(20) { index ->
                    ComicSummary(
                        id = "comic-p$page-$index",
                        title = "Comic p$page-#$index",
                        epsCount = 3,
                        categories = listOf(if (index % 2 == 0) "kept" else "other"),
                        tags = listOf("tag-$index"),
                        image = RemoteImage(),
                    )
                },
            )
        ),
    )
}
