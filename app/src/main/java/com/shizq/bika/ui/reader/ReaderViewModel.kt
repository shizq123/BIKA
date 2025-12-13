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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
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

    private val id = savedStateHandle.getStateFlow(EXTRA_ID, "")
    private val currentChapterOrder = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)

    private val _chapterMeta = MutableStateFlow<ChapterMeta?>(null)

    val uiState: StateFlow<ReaderUiState> = combine(
        userPreferencesDataSource.userData,
        currentChapterOrder,
        _chapterMeta
    ) { userData, order, meta ->
        ReaderUiState(
            readerConfig = ReaderConfig(
                volumeKeyNavigation = userData.volumeKeyNavigation,
                readingMode = userData.readingMode,
                screenOrientation = userData.screenOrientation,
                tapZoneLayout = userData.tapZoneLayout
            ),
            currentChapterOrder = order,
            chapterMeta = meta
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderUiState())

    //  图片列表流
    val imageListFlow = combine(id, currentChapterOrder, ::Pair)
        .filter { (id, _) -> id.isNotEmpty() }
        .flatMapLatest { (id, order) ->
            Pager(
                config = PagingConfig(pageSize = 10, prefetchDistance = 5)
            ) {
                chapterPagesPagingSourceFactory.create(id, order) { meta ->
                    _chapterMeta.update { meta }
                }
            }.flow
        }
        .cachedIn(viewModelScope)

    // 章节列表流 (用于侧边栏)
    val chapterListFlow = id
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            Pager(config = PagingConfig(pageSize = 20)) {
                chapterListPagingSourceFactory.create(id)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun onChapterChange(chapter: Chapter) {
        _chapterMeta.value = null
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
                chapterId = currentChapterOrder.value,
                currentPage = pageIndex,
                pageCount = totalPages,
                lastReadAt = now
            )

            historyDao.upsertChapterProgress(chapterProgress)
        }
    }

    /**
     * 保存阅读进度
     * 只需要传入 pageIndex，其余信息从 ViewModel 内部状态获取
     */
    fun saveProgress(pageIndex: Int) {
        val comicId = id.value
        val meta = _chapterMeta.value // 获取当前的元数据

        if (comicId.isEmpty() || meta == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val now = Clock.System.now()

            // 更新最后阅读时间
            val rowsUpdated = historyDao.updateLastReadAt(comicId, now)
            if (rowsUpdated <= 0) return@launch

            // 构造进度实体
            // 假设 ChapterMeta 中包含 chapterId。如果没有，你可能需要调整 Meta 类或 Paging 逻辑
//            val chapterProgress = ChapterProgressEntity(
//                historyId = comicId,
//                chapterId = "0", // 确保 ChapterMeta 里有 chapterId 字段
//                chapterNumber = meta.order,
//                currentPage = pageIndex,
//                pageCount = meta.totalImages,
//                lastReadAt = now
//            )

//            historyDao.upsertChapterProgress(chapterProgress)
        }
    }
}