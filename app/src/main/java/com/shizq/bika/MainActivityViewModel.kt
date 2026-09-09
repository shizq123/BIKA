package com.shizq.bika

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.theme.DarkThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {
    private val loginStateFlow = userCredentialsDataSource.userData
        .map { !it.token.isNullOrBlank() }

    private val themeConfigFlow = userPreferencesDataSource.userData
        .map { it.theme.darkThemeConfig }
    private val fontScaleFlow = userPreferencesDataSource.userData
        .map { it.app.fontScale }

    val uiState: StateFlow<MainActivityUiState> = combine(
        loginStateFlow,
        themeConfigFlow,
        fontScaleFlow,
    ) { isLoggedIn, darkThemeConfig, fontScale ->
        MainActivityUiState.Success(
            isLoggedIn = isLoggedIn,
            darkThemeConfig = darkThemeConfig,
            fontScale = fontScale,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    /**
     * 显式登出。
     *
     * 只清 token，保留用户名/密码，使登录页仍能预填。token 变空后
     * [loginStateFlow] 会把 [MainActivityUiState.Success.isLoggedIn] 翻成 false，
     * 根节点据此切回认证图——登出不需要导航事件。
     *
     * 在 `viewModelScope` 而不是 composition 的 scope 里执行：登出会立刻把当前
     * 页面从组合里移除，挂在页面上的协程会被取消，写入可能半途而废。
     */
    fun logout() {
        viewModelScope.launch {
            userCredentialsDataSource.setToken(null)
        }
    }
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(
        val isLoggedIn: Boolean,
        val darkThemeConfig: DarkThemeConfig,
        val fontScale: Float,
    ) : MainActivityUiState {
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
