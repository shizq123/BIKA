package com.shizq.bika.ui.signin

data class LoginUiState(
    val accountInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val rememberMe: Boolean = true,
    val isAutoSignInAttempting: Boolean = false
)