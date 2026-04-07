package com.shizq.bika

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.navigation.AuthenticationRoute
import com.shizq.bika.navigation.ConnectedRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userCredentialsDataSource: UserCredentialsDataSource,
    userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {
    private val loginStateFlow = userCredentialsDataSource.userData
        .map { !it.username.isNullOrBlank() && !it.password.isNullOrBlank() }

    private val themeConfigFlow = userPreferencesDataSource.userData
        .map { it.darkThemeConfig }

    val uiState: StateFlow<MainActivityUiState> = loginStateFlow
        .combine(themeConfigFlow) { isLoggedIn, darkThemeConfig ->
                MainActivityUiState.Success(
                    startDestination = if (isLoggedIn) ConnectedRoute else AuthenticationRoute,
                    darkThemeConfig = darkThemeConfig
                )
            }.stateIn(
                scope = viewModelScope,
                initialValue = MainActivityUiState.Loading,
                started = SharingStarted.WhileSubscribed(5_000),
            )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val startDestination: NavKey, val darkThemeConfig: DarkThemeConfig) :
        MainActivityUiState {
        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean): Boolean =
            when (darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemDarkTheme
                DarkThemeConfig.ON -> true
                DarkThemeConfig.OFF -> false
            }
    }

    fun shouldKeepSplashScreen() = this is Loading

    /**
     * Returns `true` if the dynamic color is disabled.
     */
    val shouldDisableDynamicTheming: Boolean get() = true

    /**
     * Returns `true` if the Android theme should be used.
     */
    val shouldUseAndroidTheme: Boolean get() = false
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}