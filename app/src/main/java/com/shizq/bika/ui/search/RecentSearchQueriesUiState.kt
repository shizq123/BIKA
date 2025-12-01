package com.shizq.bika.ui.search

import com.shizq.bika.core.data.model.RecentSearchQuery

sealed interface RecentSearchQueriesUiState {
    data object Loading : RecentSearchQueriesUiState

    data class Success(
        val hotKeywords: List<String> = emptyList(),
        val recentQueries: List<RecentSearchQuery> = emptyList(),
    ) : RecentSearchQueriesUiState

    data class Error(val message: String) : RecentSearchQueriesUiState
}