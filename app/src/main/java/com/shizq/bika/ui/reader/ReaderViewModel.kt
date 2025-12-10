package com.shizq.bika.ui.reader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
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
import kotlinx.coroutines.Dispatchers
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
    private val historyDao: ReadingHistoryDao,
) : ViewModel() {
    val readerPreferences = userPreferencesDataSource.userData.map {
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
    private val id = savedStateHandle.getStateFlow(EXTRA_ID, "")
    val chapterOrder = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)
    val chapterPaging = id.flatMapLatest { id ->
        Pager(config = PagingConfig(pageSize = 40)) {
            ChapterListPagingSource(id, network)
        }
            .flow
            .cachedIn(viewModelScope)
    }
    val currentPage = MutableStateFlow(0)
    val comicPaging = combine(id, chapterOrder, ::Pair)
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

    fun saveHistory2(chapterPages: LazyPagingItems<Chapter>) {
        val comicId = id.value
        val currentChapterIndex = chapterPages.peek(chapterOrder.value - 1)
        val currentPageIndex = currentPage.value
        val totalPages = PagingMetadata.totalElements.value

        if (comicId.isEmpty()) {
            Log.w(TAG, "saveHistory: Aborting, comicId is empty.")
            return
        }

        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val now = Clock.System.now()

            Log.d(
                TAG,
                "saveHistory: Saving progress for comicId '$comicId' -> Ch:$currentChapterIndex Pg:$currentPageIndex"
            )

            val rowsUpdated = historyDao.updateLastReadAt(comicId, now)
            if (rowsUpdated <= 0) {
                Log.w(TAG, "saveHistory: Parent history record not found. Skipping progress save.")
                return@launch
            }
            val chapterProgress = ChapterProgressEntity(
                historyId = comicId,
                chapterId = currentChapterIndex!!.id,
                chapterNumber = chapterOrder.value,
                currentPage = currentPageIndex,
                pageCount = totalPages,
                lastReadAt = now
            )

            historyDao.upsertChapterProgress(chapterProgress)

            Log.i(TAG, "saveHistory: Success. Updated timestamp and chapter progress.")
        }
    }

    /**
     * 保存阅读进度
     *
     * @param currentChapter 当前章节对象
     * @param pageIndex 当前阅读到的页码
     * @param totalPages 当前章节总页数
     */
    fun saveProgress(currentChapter: Chapter, pageIndex: Int, totalPages: Int) {
        val comicId = id.value
        if (comicId.isEmpty()) {
            Log.w(TAG, "saveProgress: Aborting, comicId is empty.")
            return
        }

        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val now = Clock.System.now()

            Log.d(
                TAG,
                "saveProgress: Saving $comicId -> Ch:${currentChapter.order} Pg:$pageIndex/$totalPages"
            )

            val rowsUpdated = historyDao.updateLastReadAt(comicId, now)

            if (rowsUpdated <= 0) {
                Log.w(TAG, "saveProgress: Parent history not found. Skipping.")
                return@launch
            }

            val chapterProgress = ChapterProgressEntity(
                historyId = comicId,
                chapterId = currentChapter.id,
                chapterNumber = currentChapter.order,
                currentPage = pageIndex,
                pageCount = totalPages,
                lastReadAt = now
            )

            historyDao.upsertChapterProgress(chapterProgress)
        }
    }
}

data class ChapterItemUiState(
    val chapter: Chapter,
    val isCurrent: Boolean = false
)