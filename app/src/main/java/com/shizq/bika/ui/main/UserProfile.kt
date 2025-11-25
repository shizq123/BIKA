package com.shizq.bika.ui.main

sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
    data class Success(val user: User) : UserProfileUiState
}

data class User(
    val name: String,
    val avatarUrl: String,
    val characters: List<String>,
    val level: Int,
    val exp: Int,
    val title: String,
    val gender: String,
    val slogan: String,
    val hasCheckedIn: Boolean
) {
    val levelDisplay get() = "Lv.$level"

    companion object {
        val MOCK = User(
            name = "Android Developer",
            avatarUrl = "",
            characters = emptyList(),
            level = 99,
            exp = 1000,
            title = "代码大师",
            gender = "男",
            slogan = "Hello World",
            hasCheckedIn = false,
        )
    }
}

enum class Gender {
    MALE, FEMALE, ROBOT, UNKNOWN
}