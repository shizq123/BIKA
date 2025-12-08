package com.shizq.bika.ui.reader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.core.database.dao.HistoryDao
import com.shizq.bika.core.database.model.ReadChapterEntity
import com.shizq.bika.core.database.model.ReadingProgressRecord
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ComicPagingSource
import com.shizq.bika.paging.PagingMetadata
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import com.shizq.bika.ui.reader.layout.ReaderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val TAG = "ReaderViewModel"

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
    private val historyDao: HistoryDao,
) : ViewModel() {
    val readerPreferencesFlow = userPreferencesDataSource.userData.map {
        ReaderConfig(
            volumeKeyNavigation = it.volumeKeyNavigation,
            readingMode = it.readingMode,
            screenOrientation = it.screenOrientation,
            tapZoneLayout = it.tapZoneLayout
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReaderConfig.Default
    )

    private val idFlow = savedStateHandle.getStateFlow(EXTRA_ID, "")
    val chapterIndex = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)
    val chapterPagingFlow = idFlow.flatMapLatest { id ->
        Pager(config = PagingConfig(pageSize = 40)) {
            ChapterListPagingSource(id, network)
        }
            .flow
            .cachedIn(viewModelScope)
    }
    val currentPage = MutableStateFlow(0)
    val comicPagingFlow = combine(idFlow, chapterIndex, ::Pair)
        .flatMapLatest { (id, order) ->
            Pager(config = PagingConfig(pageSize = 40)) {
                ComicPagingSource(id, order)
            }
                .flow
                .cachedIn(viewModelScope)
        }

    fun updateChapterOrder(chapter: Chapter) {
        savedStateHandle[EXTRA_ORDER] = chapter.order
    }

    fun saveHistory() {
        viewModelScope.launch(NonCancellable) {
            Log.d(TAG, "saveHistory: Attempting to save reading history...")

            val comicId = idFlow.value
            Log.d(TAG, "saveHistory: Current comicId is '$comicId'.")

            if (comicId.isEmpty()) {
                Log.w(TAG, "saveHistory: Aborting, comicId is empty.")
                return@launch
            }

            Log.d(TAG, "saveHistory: Fetching existing history from database...")
            val existingHistory = historyDao.getHistoryById(comicId)

            if (existingHistory != null) {
                Log.d(TAG, "saveHistory: Found existing history. Preparing to update.")

                val totalPages = PagingMetadata.totalElements.value
                val currentChapterOrder = chapterIndex.value
                val currentPageIndex = currentPage.value

                Log.d(
                    TAG,
                    "saveHistory: Progress to save -> Chapter: $currentChapterOrder, Page: $currentPageIndex, Total Pages: $totalPages"
                )

                val updatedHistory = existingHistory.copy(
                    lastReadAt = Clock.System.now(),
                    maxPage = totalPages,
                    lastReadProgress = ReadingProgressRecord(
                        chapterIndex = currentChapterOrder,
                        pageIndex = currentPageIndex
                    )
                )

                val readChapter = ReadChapterEntity(
                    historyId = comicId,
                    chapterIndex = currentChapterOrder
                )

                Log.d(TAG, "saveHistory: Executing database update with data: $updatedHistory")

                historyDao.updateHistoryWithChapters(updatedHistory, listOf(readChapter))

                Log.i(TAG, "saveHistory: Successfully updated history for comicId '$comicId'.")

            } else {
                Log.w(
                    TAG,
                    "saveHistory: No existing history found for comicId '$comicId'. Nothing to update."
                )
            }
        }
    }

    override fun onCleared() {
        saveHistory()
    }
}

data class ChapterItemUiState(
    val chapter: Chapter,
    val isCurrent: Boolean = false
)