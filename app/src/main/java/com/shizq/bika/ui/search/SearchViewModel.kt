package com.shizq.bika.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.RecentSearchRepository
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.KeywordsResponse
import com.shizq.bika.network.Result
import com.shizq.bika.network.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val networkApi: BikaDataSource,
    private val recentSearchRepository: RecentSearchRepository,
) : ViewModel() {
    private val recentSearchesFlow = recentSearchRepository.getRecentSearchQueries(limit = 256)

    private val keywordsResultFlow: Flow<Result<KeywordsResponse>> = flow {
        emit(networkApi.getKeywords())
    }.asResult()

    val recentSearchQueriesUiState = combine(
        keywordsResultFlow,
        recentSearchesFlow
    ) { keywordsResult, recentSearches ->
        when (keywordsResult) {
            is Result.Loading -> RecentSearchQueriesUiState.Loading
            is Result.Error -> RecentSearchQueriesUiState.Error(
                keywordsResult.exception?.message ?: ""
            )

            is Result.Success -> RecentSearchQueriesUiState.Success(
                hotKeywords = keywordsResult.data.keywords,
                recentQueries = recentSearches
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecentSearchQueriesUiState.Loading
        )

    fun onSearchTriggered(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            recentSearchRepository.insertOrReplaceRecentSearch(searchQuery = query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearRecentSearches()
        }
    }
}