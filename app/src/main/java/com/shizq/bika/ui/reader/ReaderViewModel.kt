package com.shizq.bika.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ChapterPagesPagingSource
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderUiState
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
    readerStateMachine: ReaderStateMachine,
    @Assisted id: String,
    @Assisted order: Int
) : ViewModel() {
    private val currentChapterOrder = savedStateHandle.getStateFlow("order", order)

    init {
        readerStateMachine.initializeWith { ReaderUiState.Initializing(id, order) }
    }

    private val stateMachine = readerStateMachine.launchIn(viewModelScope)
    val stateFlow = stateMachine.state

    //  图片列表流
    val imageListFlow = currentChapterOrder
        .flatMapLatest { order ->
            Pager(PagingConfig(40)) {
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