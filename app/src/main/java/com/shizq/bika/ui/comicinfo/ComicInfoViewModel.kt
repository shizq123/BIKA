package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.database.dao.ReadingHistoryDao
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
    private val historyDao: ReadingHistoryDao,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val unitedDetailsStateMachine: UnitedDetailsStateMachine,
) : ViewModel() {
    private val restarter = FlowRestarter()
    private val comicIdFlow = savedStateHandle.getStateFlow("id", "")

    private val stateMachine = unitedDetailsStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state
    val episodesFlow: Flow<PagingData<Episode>> = comicIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrEmpty()) {
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

    fun retry() {
        restarter.restart()
    }

    fun toggleCommentLike(id: String) {
//        val currentState = comicDetailUiState.value
//        if (currentState is ComicDetailUiState.Success) {
//            viewModelScope.launch {
//                network.toggleCommentLike(id)
//            }
//        }
    }

    fun toggleLike() {
//        val currentState = comicDetailUiState.value
//        if (currentState is ComicDetailUiState.Success) {
//            val originalState = currentState.detail.isLiked
//            val newState = !originalState
//
//            likeStateOverride.update { newState }
//
//            viewModelScope.launch {
//                try {
//                    network.toggleLike(currentState.detail.id)
//                } catch (e: Exception) {
//                    likeStateOverride.update { originalState }
//                }
//            }
//        }
    }

    fun toggleFavorite() {
//        val currentState = comicDetailUiState.value
//        if (currentState is ComicDetailUiState.Success) {
//            val originalFavoriteState = currentState.detail.isFavourite
//            val newFavoriteState = !originalFavoriteState
//
//            favoriteStateOverride.update { newFavoriteState }
//
//            viewModelScope.launch {
//                try {
//                    network.toggleFavourite(currentState.detail.id)
//                } catch (e: Exception) {
//                    favoriteStateOverride.update { originalFavoriteState }
//                }
//            }
//        }
    }

    fun recordVisit() {
//        // 1. 使用卫语句（Guard Clause）提取状态，减少嵌套
//        val currentState = comicDetailUiState.value
//        if (currentState !is ComicDetailUiState.Success) return
//
//        val detail = currentState.detail
//
//        viewModelScope.launch(Dispatchers.IO) {
//            val now = Clock.System.now()
//
//            val rowsUpdated = historyDao.updateLastReadAt(detail.id, now)
//
//            if (rowsUpdated > 0) {
//                Log.d(TAG, "recordVisit: History exists. Updated timestamp for '${detail.title}'.")
//            } else {
//                Log.d(
//                    TAG,
//                    "recordVisit: No history found. Creating new record for '${detail.title}'."
//                )
//                val newRecord = ReadingHistoryEntity(
//                    id = detail.id,
//                    title = detail.title,
//                    author = detail.author,
//                    coverUrl = detail.cover,
//                    lastInteractionAt = now
//                )
//                historyDao.upsertHistory(newRecord)
//            }
//        }
    }
}