package com.shizq.bika.ui.reader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.paging.ChapterPagesPagingSource
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val TAG = "ReaderViewModel"

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val historyDao: ReadingHistoryDao,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory
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
    val currentChapterOrder = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)
    val chapterMeta = MutableStateFlow<ChapterMeta?>(null)

    // 图片列表
    val imageListFlow = combine(id, currentChapterOrder, ::Pair).flatMapLatest { (id, order) ->
        Pager(config = PagingConfig(pageSize = 40)) {
            chapterPagesPagingSourceFactory.create(id, order) { meta ->
                chapterMeta.update { meta }
            }
        }
            .flow
            .cachedIn(viewModelScope)
    }

    // 当前可见项
    val currentPageIndex = MutableStateFlow(0)

    // 章节列表
    val chapterListFlow = id.flatMapLatest { id ->
        Pager(config = PagingConfig(pageSize = 40)) {
            chapterListPagingSourceFactory.create(id)
        }
            .flow
            .cachedIn(viewModelScope)
    }

    fun onChapterChange(chapter: Chapter) {
        savedStateHandle[EXTRA_ORDER] = chapter.order
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