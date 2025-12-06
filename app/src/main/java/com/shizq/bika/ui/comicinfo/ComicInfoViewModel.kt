package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.paging.EpisodePagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ComicInfoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
) : ViewModel() {
    private val comicIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow("id", null)
    val comicDetailUiState: StateFlow<ComicDetailUiState> = comicIdFlow
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(Result.Loading)
            } else {
                flow { emit(network.getComicDetails(id)) }.asResult()
            }
        }
        .map { result ->
            when (result) {
                is Result.Success -> ComicDetailUiState.Success(result.data.toComicDetail())
                is Result.Error -> ComicDetailUiState.Error(
                    result.exception.message ?: "Unknown error"
                )

                Result.Loading -> ComicDetailUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ComicDetailUiState.Loading
        )

    val recommendationsUiState: StateFlow<RecommendationsUiState> = comicIdFlow
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(Result.Loading)
            } else {
                flow { emit(network.getRecommendations(id)) }.asResult()
            }
        }
        .map { result ->
            when (result) {
                is Result.Success -> RecommendationsUiState.Success(result.data.toComicSummaryList())
                is Result.Error -> RecommendationsUiState.Error(
                    result.exception.message ?: "Unknown error"
                )

                Result.Loading -> RecommendationsUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecommendationsUiState.Loading
        )
    val episodesFlow: Flow<PagingData<Episode>> = comicIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrEmpty()) {
                emptyFlow()
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 40,
                        prefetchDistance = 5,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = { EpisodePagingSource(network, id) }
                ).flow
            }
        }
        .cachedIn(viewModelScope)
}
