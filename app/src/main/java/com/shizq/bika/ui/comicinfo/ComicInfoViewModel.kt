package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.paging.EpisodePagingSource
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.paging.ReplyPagingSource
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "ComicInfoViewModel"

@HiltViewModel(assistedFactory = ComicInfoViewModel.Factory::class)
class ComicInfoViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val replyPagingSourceFactory: ReplyPagingSource.Factory,
    unitedDetailsStateMachine: UnitedDetailsStateMachine,
    @Assisted val id: String,
) : ViewModel() {
    init {
        unitedDetailsStateMachine.initializeWith { UnitedDetailsUiState.Initialize(id) }
    }

    private val stateMachine = unitedDetailsStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state
    val episodesFlow: Flow<PagingData<Episode>> = Pager(PagingConfig(40)) {
        EpisodePagingSource(network, id)
    }
        .flow
        .cachedIn(viewModelScope)
    val pinnedComments: StateFlow<List<Comment>>
        field = MutableStateFlow(emptyList())
    val regularComments: Flow<PagingData<Comment>> = Pager(PagingConfig(40)) {
        commentPagingSourceFactory(id) {
            pinnedComments.value = it
        }
    }.flow

    val replyList = state.map {
        (it as? UnitedDetailsUiState.Content)?.viewingRepliesForId
    }.filterNotNull()
        .flatMapLatest {
            Pager(PagingConfig(5)) {
                replyPagingSourceFactory(it)
            }.flow
        }.cachedIn(viewModelScope)

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
    fun toggleCommentLike(id: String) {
//        val currentState = comicDetailUiState.value
//        if (currentState is ComicDetailUiState.Success) {
//            viewModelScope.launch {
//                network.toggleCommentLike(id)
//            }
//        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
        ): ComicInfoViewModel
    }
}