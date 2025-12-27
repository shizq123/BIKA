package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.paging.EpisodePagingSource
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private const val TAG = "ComicInfoViewModel"

@HiltViewModel
class ComicInfoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val unitedDetailsStateMachine: UnitedDetailsStateMachine,
) : ViewModel() {
    private val comicIdFlow = savedStateHandle.getStateFlow("id", "")

    private val stateMachine = unitedDetailsStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state
    val episodesFlow: Flow<PagingData<Episode>> = comicIdFlow
        .flatMapLatest { id ->
            if (id.isEmpty()) {
                emptyFlow()
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 40,
                    ),
                    pagingSourceFactory = { EpisodePagingSource(network, id) }
                ).flow
            }
        }
        .cachedIn(viewModelScope)
    val pinnedComments: StateFlow<List<Comment>>
        field = MutableStateFlow(emptyList())
    val regularComments: Flow<PagingData<Comment>> = comicIdFlow
        .flatMapLatest { id ->
            if (id.isEmpty()) {
                emptyFlow()
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 40,
                    ),
                ) {
                    commentPagingSourceFactory(id) {
                        pinnedComments.value = it
                    }
                }.flow
            }
        }

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
}