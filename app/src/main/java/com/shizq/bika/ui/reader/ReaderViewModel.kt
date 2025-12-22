package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.paging.ChapterPagesPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ID
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.statemachine.ReaderStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private const val TAG = "ReaderViewModel"

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory,
    private val readerStateMachineFactory: ReaderStateMachine.Factory
) : ViewModel() {
    private val id = savedStateHandle.getStateFlow(EXTRA_ID, "")
    private val currentChapterOrder = savedStateHandle.getStateFlow(EXTRA_ORDER, 1)

    private val _chapterMeta = MutableStateFlow<ChapterMeta?>(null)

    private val stateMachineFactory = readerStateMachineFactory(id.value, currentChapterOrder.value)
    private val stateMachine = stateMachineFactory.launchIn(viewModelScope)
    val stateFlow = stateMachine.state

    //  图片列表流
    val imageListFlow = combine(id, currentChapterOrder, ::Pair)
        .filter { (id, _) -> id.isNotEmpty() }
        .flatMapLatest { (id, order) ->
            Pager(
                config = PagingConfig(pageSize = 10, prefetchDistance = 5)
            ) {
                chapterPagesPagingSourceFactory.create(id, order) { meta ->
                    dispatch(ReaderAction.OnMetaLoaded(meta))
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
    //    viewModelScope.launch {
    //            state
    //                .filterIsInstance<ReaderUiState.Ready>()
    //                .map { it.id to it.chapter.order } // 注意这里路径变了：it.chapter.order
    //                .distinctUntilChanged()
    //                .collect { (id, order) ->
    //                    dispatch(ReaderAction.LoadHistory(id, order))
    //                }
    //        }

    fun onChapterChange(chapter: Chapter) {
        _chapterMeta.value = null
        savedStateHandle[EXTRA_ORDER] = chapter.order
    }

    fun onReadingModeChange(mode: ReadingMode) {
        viewModelScope.launch {
            userPreferencesDataSource.setReadingMode(mode)
        }
    }

    fun dispatch(action: ReaderAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}