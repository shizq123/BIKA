package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.network.model.Gender

sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
    data class Success(val user: User, val isOfflineCache: Boolean = false) : UserProfileUiState
}

data class User(
    val name: String,
    val avatarUrl: String,
    val characters: List<String>,
    val level: Int,
    val exp: Int,
    val title: String,
    val gender: Gender,
    val slogan: String,
    val hasCheckedIn: Boolean
) {
    val levelDisplay get() = "Lv.$level"
}