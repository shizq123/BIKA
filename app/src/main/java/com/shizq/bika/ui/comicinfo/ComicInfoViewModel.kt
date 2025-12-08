package com.shizq.bika.ui.comicinfo

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.database.dao.HistoryDao
import com.shizq.bika.core.database.model.HistoryRecordEntity
import com.shizq.bika.core.database.model.ReadingProgressRecord
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.paging.EpisodePagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val TAG = "ComicInfoViewModel"

@HiltViewModel
class ComicInfoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
    private val historyDao: HistoryDao,
) : ViewModel() {
    private val restarter = FlowRestarter()
    private val comicIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow("id", null)
    private val favoriteStateOverride = MutableStateFlow<Boolean?>(null)
    private val likeStateOverride = MutableStateFlow<Boolean?>(null)
    val comicDetailUiState: StateFlow<ComicDetailUiState> = combine(
        comicIdFlow
            .flatMapLatest { id ->
                favoriteStateOverride.value = null
                likeStateOverride.value = null

                if (id == null) {
                    flowOf(Result.Loading)
                } else {
                    flow { emit(network.getComicDetails(id)) }.asResult()
                }
            },
        favoriteStateOverride,
        likeStateOverride
    ) { result, isFavoriteOverride, isLikeOverride ->
        when (result) {
            is Result.Success -> {
                val originalDetail = result.data.toComicDetail()

                val finalDetail = originalDetail.copy(
                    isFavourite = isFavoriteOverride ?: originalDetail.isFavourite,
                    isLiked = isLikeOverride ?: originalDetail.isLiked
                )
                ComicDetailUiState.Success(finalDetail)
            }

            is Result.Error -> ComicDetailUiState.Error(
                result.exception.message ?: "Unknown error"
            )

            Result.Loading -> ComicDetailUiState.Loading
        }
    }
        .restartable(restarter)
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

    fun retry() {
        restarter.restart()
    }

    fun toggleLike() {
        val currentState = comicDetailUiState.value
        if (currentState is ComicDetailUiState.Success) {
            val originalState = currentState.detail.isLiked
            val newState = !originalState

            likeStateOverride.update { newState }

            viewModelScope.launch {
                try {
                    network.toggleLike(currentState.detail.id)
                } catch (e: Exception) {
                    likeStateOverride.update { originalState }
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = comicDetailUiState.value
        if (currentState is ComicDetailUiState.Success) {
            val originalFavoriteState = currentState.detail.isFavourite
            val newFavoriteState = !originalFavoriteState

            favoriteStateOverride.update { newFavoriteState }

            viewModelScope.launch {
                try {
                    network.toggleFavourite(currentState.detail.id)
                } catch (e: Exception) {
                    favoriteStateOverride.update { originalFavoriteState }
                }
            }
        }
    }

    fun recordVisit() {
        Log.d(TAG, "recordVisit: Function called.")

        val currentState = comicDetailUiState.value
        if (currentState is ComicDetailUiState.Success) {
            Log.d(TAG, "recordVisit: UI state is 'Success'. Launching coroutine to record visit.")

            viewModelScope.launch {
                val detail = currentState.detail
                val comicId = detail.id

                Log.d(TAG, "recordVisit: Processing comicId: '$comicId'")

                Log.d(TAG, "recordVisit: Checking for existing history record...")
                val existingHistory = historyDao.getHistoryById(comicId)

                val lastReadProgress = if (existingHistory != null) {
                    Log.d(
                        TAG,
                        "recordVisit: Found existing history. Progress will be preserved: ${existingHistory.lastReadProgress}"
                    )
                    existingHistory.lastReadProgress
                } else {
                    Log.w(
                        TAG,
                        "recordVisit: No existing history found. Creating new record with default progress (Chapter 0, Page 0)."
                    )
                    ReadingProgressRecord(chapterIndex = 0, pageIndex = 0)
                }

                // 构建将要写入数据库的完整对象
                val historyRecord = HistoryRecordEntity(
                    id = detail.id,
                    title = detail.title,
                    author = detail.author,
                    cover = detail.cover,
                    lastReadAt = Clock.System.now(),
                    maxPage = existingHistory?.maxPage, // 保留已有的 maxPage
                    lastReadProgress = lastReadProgress
                )

                Log.d(TAG, "recordVisit: Preparing to upsert the following data: $historyRecord")

                historyDao.upsertHistoryRecord(historyRecord)

                Log.i(TAG, "recordVisit: Successfully upserted history for comicId '$comicId'.")
            }
        } else {
            Log.w(
                TAG,
                "recordVisit: Skipped. UI state is not 'Success', current state is ${currentState::class.simpleName}."
            )
        }
    }
}