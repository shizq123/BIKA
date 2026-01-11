package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ChapterPagesPagingSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.statemachine.ReaderStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private const val TAG = "ReaderViewModel"

@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory,
    readerStateMachineFactory: ReaderStateMachine.Factory,
    @Assisted id: String,
    @Assisted order: Int
) : ViewModel() {
    private val currentChapterOrder = savedStateHandle.getStateFlow(EXTRA_ORDER, order)

    private val stateMachineFactory = readerStateMachineFactory(id, currentChapterOrder.value)
    private val stateMachine = stateMachineFactory.launchIn(viewModelScope)
    val stateFlow = stateMachine.state

    //  图片列表流
    val imageListFlow = currentChapterOrder
        .flatMapLatest { order ->
            Pager(
                config = PagingConfig(pageSize = 40, prefetchDistance = 5, initialLoadSize = 40)
            ) {
                chapterPagesPagingSourceFactory.create(id, order) { meta ->
                    dispatch(ReaderAction.ChapterMetaLoaded(meta))
                }
            }.flow
        }
        .cachedIn(viewModelScope)

    // 章节列表流 (用于侧边栏)
    val chapterListFlow = Pager(config = PagingConfig(pageSize = 20)) {
        chapterListPagingSourceFactory.create(id)
    }.flow
        .cachedIn(viewModelScope)

    fun dispatch(action: ReaderAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(id: String, order: Int): ReaderViewModel
    }
}