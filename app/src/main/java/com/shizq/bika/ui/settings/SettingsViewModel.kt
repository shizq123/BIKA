package com.shizq.bika.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.NetworkLine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import java.text.DecimalFormat

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext application: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource
) : ViewModel() {
    val settingsUiState = userPreferencesDataSource.userData.map {
        SettingsUiState.Success(
            darkThemeConfig = it.darkThemeConfig,
            selectedNetworkLine = it.selectedNetworkLine,
            autoCheckIn = it.autoCheckIn,
            fontScale = it.fontScale,
            isLoggingEnabled = it.isLoggingEnabled
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState.Loading
    )
    private val imageLoader = application.imageLoader

    val cacheSize: StateFlow<String>
        field = MutableStateFlow("计算中...")

    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    init {
        updateCacheSize()
    }

    fun checkForUpdates() {
        if (_updateUiState.value is UpdateUiState.Checking) return
        _updateUiState.value = UpdateUiState.Checking

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/STlxx-lin/BIKA/releases/tags/latest")
                    .header("User-Agent", "BIKA-Android")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP 错误码: ${response.code}")
                    val bodyString = response.body.string() ?: throw Exception("响应内容为空")
                    val json = Json.parseToJsonElement(bodyString).jsonObject
                    val tagName = json["tag_name"]?.jsonPrimitive?.content ?: throw Exception("未找到 tag_name")
                    val releaseNotes = json["body"]?.jsonPrimitive?.content ?: ""
                    val htmlUrl = json["html_url"]?.jsonPrimitive?.content ?: "https://github.com/STlxx-lin/BIKA/releases"

                    val assets = json["assets"]?.jsonArray
                    var apkUrl = htmlUrl
                    var remoteVersion = ""
                    if (assets != null) {
                        for (i in 0 until assets.size) {
                            val assetObj = assets[i].jsonObject
                            val name = assetObj["name"]?.jsonPrimitive?.content ?: ""
                            if (name.endsWith(".apk") && name.contains("_v")) {
                                remoteVersion = name.substringAfter("_v").substringBefore(".apk")
                                apkUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content ?: htmlUrl
                                break
                            }
                        }
                    }

                    if (remoteVersion.isEmpty()) {
                        remoteVersion = tagName.trimStart('v')
                    }
                    val currentVersion = com.shizq.bika.BuildConfig.VERSION_NAME

                    if (isNewerVersion(remoteVersion, currentVersion)) {
                        _updateUiState.value = UpdateUiState.HasUpdate(remoteVersion, releaseNotes, apkUrl)
                    } else {
                        _updateUiState.value = UpdateUiState.NoUpdate
                    }
                }
            } catch (e: Exception) {
                _updateUiState.value = UpdateUiState.Error(e.message ?: "未知网络错误")
            }
        }
    }

    fun resetUpdateState() {
        _updateUiState.value = UpdateUiState.Idle
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

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

    fun clearLogs() {
        com.shizq.bika.core.common.BikaLog.clearLogs()
    }

    fun getLogsContent(): String {
        return try {
            val logFile = com.shizq.bika.core.common.BikaLog.getLogFile()
            if (logFile != null && logFile.exists()) {
                val lines = logFile.readLines()
                if (lines.size > 2000) {
                    "【日志已截断，仅展示最后 2000 行】\n\n" + lines.takeLast(2000).joinToString("\n")
                } else {
                    lines.joinToString("\n")
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            "读取日志失败: ${e.localizedMessage}"
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
        val autoCheckIn: Boolean,
        val fontScale: Float,
        val isLoggingEnabled: Boolean
    ) : SettingsUiState
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class HasUpdate(val version: String, val body: String, val url: String) : UpdateUiState
    data object NoUpdate : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}