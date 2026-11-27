package com.shizq.bika.feature.reader.impl.statemachine

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.preferences.ReaderPreferences
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.feature.reader.impl.state.ReaderUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderStateMachineTest {

    @Test
    fun `first ready state already carries persisted preloadCount and readingMode`() = runTest {
        val preferences = UserPreferencesDataSource(
            InMemoryUserPreferencesDataStore(
                UserPreferences(
                    reader = ReaderPreferences(
                        readingMode = ReadingMode.LEFT_TO_RIGHT,
                        preloadCount = 9,
                    )
                )
            )
        )
        val machineFactory = ReaderStateMachine(
            savedStateHandle = SavedStateHandle(),
            userPreferencesDataSource = preferences,
            historyDao = EmptyReadingHistoryDao,
        )
        machineFactory.initializeWith { ReaderUiState.Initializing(id = "comic-1", order = 1) }

        val machine = machineFactory.launchIn(backgroundScope)
        val firstReady = machine.state.filterIsInstance<ReaderUiState.Ready>().first()

        assertEquals(9, firstReady.config.preloadCount)
        assertEquals(ReadingMode.LEFT_TO_RIGHT, firstReady.config.readingMode)
    }

    private class InMemoryUserPreferencesDataStore(
        initial: UserPreferences,
    ) : DataStore<UserPreferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UserPreferences> = state

        override suspend fun updateData(
            transform: suspend (t: UserPreferences) -> UserPreferences,
        ): UserPreferences = state.updateAndGet { transform(it) }
    }

    private object EmptyReadingHistoryDao : ReadingHistoryDao {
        override suspend fun upsertHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun upsertHistories(histories: List<ReadingHistoryEntity>) = Unit
        override suspend fun deleteHistory(history: ReadingHistoryEntity) = Unit
        override suspend fun deleteHistoryById(id: String) = Unit
        override suspend fun clearAllHistories() = Unit
        override suspend fun upsertChapterProgress(progress: ChapterProgressEntity) = Unit
        override suspend fun upsertChapterProgressList(progressList: List<ChapterProgressEntity>) = Unit
        override fun getDetailedHistories(): Flow<List<DetailedHistory>> = MutableStateFlow(emptyList())
        override fun getLatestDetailedHistory(): Flow<DetailedHistory?> = MutableStateFlow(null)
        override suspend fun getDetailedHistoryById(id: String): DetailedHistory? = null
        override suspend fun updateLastReadAt(id: String, newTimestamp: Instant): Int = 0
        override suspend fun updateIsFavourited(id: String, isFavourited: Boolean): Int = 0
        override fun getChapterProgressByComic(comicId: String): Flow<List<ChapterProgressEntity>> = MutableStateFlow(emptyList())
        override fun getAllChapterProgress(): Flow<List<ChapterProgressEntity>> = MutableStateFlow(emptyList())
    }
}
