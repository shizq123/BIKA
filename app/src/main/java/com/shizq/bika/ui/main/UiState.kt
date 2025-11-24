package com.shizq.bika.ui.main

sealed class UiState {
    object Loading : UiState()
    data class Success(val categories: List<CategoryItem>) : UiState()
    data class Error(val message: String) : UiState()
}