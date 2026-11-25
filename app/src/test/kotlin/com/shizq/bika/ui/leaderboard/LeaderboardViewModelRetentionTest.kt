package com.shizq.bika.ui.leaderboard

import androidx.datastore.core.DataStore
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.KnightLeaderboardData
import com.shizq.bika.core.network.model.LeaderboardData
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelRetentionTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `离开排行榜超过五秒再返回不重置为加载态且不重复请求网络`() = runTest(dispatcher) {
        val requestCount = AtomicInteger(0)
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = this@LeaderboardViewModelRetentionTest.dispatcher
            requestHandlers.add { request ->
                requestCount.incrementAndGet()
                val payload = if (request.url.encodedPath.contains("knight")) {
                    Json.encodeToString(KnightLeaderboardData(users = emptyList()))
                } else {
                    Json.encodeToString(
                        LeaderboardData(
                            comics = listOf(
                                ComicSummary(
                                    id = "lb-1",
                                    title = "Leaderboard Comic 1",
                                    image = RemoteImage(),
                                ),
                            ),
                        ),
                    )
                }
                respond(payload, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        val client = HttpClient(engine) {
            defaultRequest {
                url("https://leaderboard.invalid/")
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
            }
        }
        val vm = LeaderboardViewModel(
            api = BikaDataSource(client),
            historyDao = FakeReadingHistoryDao(),
            userPreferencesDataSource = UserPreferencesDataSource(FakePreferencesStore()),
        )

        val firstSub = backgroundScope.launch(dispatcher) { vm.leaderboardUiState.collect() }
        runCurrent()
        assertIs<LeaderboardUiState.Success>(vm.leaderboardUiState.value)
        assertEquals(4, requestCount.get(), "初次进入应请求日榜、周榜、月榜、骑士榜各一次")

        // 模拟进入漫画详情页并停留超过 5 秒（旧实现 WhileSubscribed(5_000) 会取消上游流）
        firstSub.cancel()
        advanceTimeBy(10_000)
        runCurrent()

        // 模拟返回排行榜重新订阅
        val emitted = mutableListOf<LeaderboardUiState>()
        val secondSub = backgroundScope.launch(dispatcher) {
            vm.leaderboardUiState.collect { emitted += it }
        }
        runCurrent()

        assertEquals(listOf(vm.leaderboardUiState.value), emitted)
        assertIs<LeaderboardUiState.Success>(vm.leaderboardUiState.value)
        assertEquals(4, requestCount.get(), "返回排行榜不应重新发起网络请求")

        secondSub.cancel()
        vm.viewModelScope.cancel()
        client.close()
        engine.close()
    }

    private class FakePreferencesStore : DataStore<UserPreferences> {
        private val state = MutableStateFlow(UserPreferences())
        override val data: Flow<UserPreferences> = state
        override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
            transform(state.value).also { state.value = it }
    }

    private class FakeReadingHistoryDao : ReadingHistoryDao {
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
}
