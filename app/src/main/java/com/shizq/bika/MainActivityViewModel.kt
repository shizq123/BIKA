package com.shizq.bika

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.DarkThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userCredentialsDataSource: UserCredentialsDataSource,
    userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> =
        userCredentialsDataSource.userData
            .combine(userPreferencesDataSource.userData) { credentials, preferences ->
                MainActivityUiState.Success(
                    isLoggedIn = credentials.username != null && credentials.password != null,
                    darkThemeConfig = preferences.darkThemeConfig
                )
            }.stateIn(
                scope = viewModelScope,
                initialValue = MainActivityUiState.Loading,
                started = SharingStarted.WhileSubscribed(5_000),
            )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val isLoggedIn: Boolean, val darkThemeConfig: DarkThemeConfig) :
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