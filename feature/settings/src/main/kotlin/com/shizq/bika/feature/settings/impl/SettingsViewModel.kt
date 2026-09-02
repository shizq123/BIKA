package com.shizq.bika.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.model.theme.DarkThemeConfig
import com.shizq.bika.core.network.image.ImageCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val imageCacheManager: ImageCacheManager,
    private val messageReporter: MessageReporter,
) : ViewModel() {
    val settingsUiState = userPreferencesDataSource.userData.map {
        SettingsUiState.Success(
            darkThemeConfig = it.theme.darkThemeConfig,
            autoCheckIn = it.app.autoCheckIn,
            fontScale = it.app.fontScale,
            isLoggingEnabled = it.network.isLoggingEnabled,
            downloadOverWifiOnly = it.download.overWifiOnly,
            maxConcurrentDownloads = it.download.maxConcurrentDownloads,
            secureScreenEnabled = it.app.secureScreenEnabled,
            usePredictiveBack = it.app.predictiveBackEnabled
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState.Loading
    )

    private val _cacheSize = MutableStateFlow("计算中...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
    }

    // 使用 ApplicationScope：确保退出登录时即使页面已销毁，清除 token 的操作也能完成
    fun logout() {
        scope.launch {
            userCredentialsDataSource.setToken(null)
        }
    }

    fun updateDarkThemeConfig(config: DarkThemeConfig) {
        viewModelScope.launch {
            userPreferencesDataSource.setDarkThemeConfig(config)
        }
    }

    fun updateAutoCheckIn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setAutoCheckIn(enabled)
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            userPreferencesDataSource.setFontScale(scale)
        }
    }

    fun updateIsLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setIsLoggingEnabled(enabled)
        }
    }

    fun updateSecureScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setSecureScreenEnabled(enabled)
        }
    }

    fun updateUsePredictiveBack(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setUsePredictiveBack(enabled)
        }
    }

    fun updateDownloadOverWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setDownloadOverWifiOnly(enabled)
        }
    }

    fun updateMaxConcurrentDownloads(count: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.setMaxConcurrentDownloads(count)
        }
    }

    fun clearLogs() {
        BikaLog.clearLogs()
    }

    suspend fun getLogsContent(): String =
        withContext(Dispatchers.IO) {
            try {
                val logFile = BikaLog.getLogFile()
                if (logFile != null && logFile.exists()) {
                    val lines = logFile.readLines()
                    if (lines.size > 2000) {
                        "【日志已截断，仅展示最后 2000 行】\n\n" + lines.takeLast(2000)
                            .joinToString("\n")
                    } else {
                        lines.joinToString("\n")
                    }
                } else {
                    ""
                }
            } catch (e: Exception) {
                BikaLog.e(TAG, "读取日志失败", e)
                "读取日志失败: ${e.localizedMessage}"
            }
        }

    /**
     * 在后台线程更新缓存大小，并更新 StateFlow
     */
    fun updateCacheSize() {
        viewModelScope.launch {
            val size = imageCacheManager.diskCacheSize()
            _cacheSize.value = formatBytes(size)
        }
    }

    /**
     * 在后台线程清理缓存，并在完成后刷新缓存大小
     */
    fun clearCache() {
        viewModelScope.launch {
            imageCacheManager.clear()
            updateCacheSize()
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private val logger = KotlinLogging.logger { "SettingsViewModel" }
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val darkThemeConfig: DarkThemeConfig,
        val autoCheckIn: Boolean,
        val fontScale: Float,
        val isLoggingEnabled: Boolean,
        val downloadOverWifiOnly: Boolean,
        val maxConcurrentDownloads: Int,
        val secureScreenEnabled: Boolean,
        val usePredictiveBack: Boolean
    ) : SettingsUiState
}

/**
 * 将字节数格式化为可读的字符串 (B, KB, MB, GB)。
 *
 * 提取为顶层纯函数以便单独进行单元测试，不依赖 [SettingsViewModel] 的构造参数。
 */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    val kb = bytes / 1024.0
    if (kb < 1024) return "${decimalFormat.format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${decimalFormat.format(mb)} MB"
    val gb = mb / 1024.0
    return "${decimalFormat.format(gb)} GB"
}
