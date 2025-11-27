package com.shizq.bika.ui.main

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val name: String,
//        val dashboardEntries: List<DashboardEntry>,
//        val selectedTopicId: String?,
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}