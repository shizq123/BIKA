package com.shizq.bika.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.NetworkLine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext application: Context,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {
    val settingsUiState = userPreferencesDataSource.userData.map {
        SettingsUiState.Success(it.darkThemeConfig, it.selectedNetworkLine, it.autoCheckIn)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState.Loading
    )
    private val imageLoader = application.imageLoader

    val cacheSize: StateFlow<String>
        field = MutableStateFlow("计算中...")

    init {
        updateCacheSize()
    }

    fun updateDarkThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch {
            userPreferencesDataSource.setDarkThemeConfig(config)
        }
    }

    fun updateSelectedNetworkLine(line: NetworkLine) {
        viewModelScope.launch {
            userPreferencesDataSource.setNetworkLine(line)
        }
    }

    fun updateAutoCheckIn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setAutoCheckIn(enabled)
        }
    }

    /**
     * 在后台线程更新缓存大小，并更新 StateFlow
     */
    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = imageLoader.diskCache?.size ?: 0L
            val formattedSize = formatBytes(size)
            cacheSize.value = formattedSize
        }
    }

    /**
     * 在后台线程清理缓存，并在完成后刷新缓存大小
     */
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            imageLoader.diskCache?.clear()
            updateCacheSize()
        }
    }

    /**
     * 将字节数格式化为可读的字符串 (KB, MB, GB)
     */
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${DecimalFormat("#.##").format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "${DecimalFormat("#.##").format(mb)} MB"
        val gb = mb / 1024.0
        return "${DecimalFormat("#.##").format(gb)} GB"
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val darkThemeConfig: DarkThemeConfig,
        val selectedNetworkLine: NetworkLine,
        val autoCheckIn: Boolean
    ) : SettingsUiState
}