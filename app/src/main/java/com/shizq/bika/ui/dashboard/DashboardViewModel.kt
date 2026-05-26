package com.shizq.bika.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.content.Intent
import com.shizq.bika.core.coroutine.restartable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CheckInEvent {
    data class Success(val message: String) : CheckInEvent
    data class Error(val error: String) : CheckInEvent
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
) : ViewModel() {
    private val dashboardRestarter = FlowRestarter()

    private val _checkInEvent = MutableSharedFlow<CheckInEvent>()
    val checkInEvent = _checkInEvent.asSharedFlow()

    val userChannelPreferences = userPreferencesDataSource.userData
        .map { preferences ->
            preferences.channels.filter { it.isActive }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )
    val userProfileUiState = flow {
        emit(network.fetchUserProfile())
    }.asResult()
        .restartable(dashboardRestarter)
        .map { result ->
            when (result) {
                Result.Loading -> UserProfileUiState.Loading
                is Result.Error -> UserProfileUiState.Error(result.exception.message ?: "")
                is Result.Success -> {
                    val user = result.data.user
                    UserProfileUiState.Success(
                        User(
                            name = user.name,
                            avatarUrl = user.imageUrl,
                            characters = user.characters,
                            level = user.level,
                            exp = user.exp,
                            title = user.title,
                            gender = user.gender,
                            slogan = user.slogan,
                            hasCheckedIn = user.isPunched,
                        )
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfileUiState.Loading
        )

    fun restart() {
        dashboardRestarter.restart()
    }

    // performAutoCheckIn
    // performInitialLogin
    fun onCheckIn(isAuto: Boolean = false) {
        viewModelScope.launch {
//            if (!userPreferencesDataSource.userData.first().autoCheckIn) {
//                return@launch
//            }
            try {
                network.punchIn()
                val msg = if (isAuto) "自动签到成功！已成功打哔咔。" else "打卡成功！已成功打哔咔。"
                _checkInEvent.emit(CheckInEvent.Success(msg))
                restart()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "签到失败", e)
                _checkInEvent.emit(CheckInEvent.Error("打卡失败：${e.localizedMessage ?: "未知错误"}"))
            }
        }
    }

    // ================== 自动更新检测与下载安装机制 ==================

    sealed interface UpdateUiState {
        data object Idle : UpdateUiState
        data class HasUpdate(val remoteVersion: String, val changelog: String, val downloadUrl: String) : UpdateUiState
        data class Downloading(val progress: Float) : UpdateUiState
        data class Success(val apkFile: java.io.File) : UpdateUiState
        data class Error(val message: String) : UpdateUiState
    }

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState = _updateState.asStateFlow()

    fun resetUpdateState() {
        _updateState.value = UpdateUiState.Idle
    }

    fun checkUpdate(currentVersionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/STlxx-lin/BIKA/releases/tags/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", "BIKA-Android-App")

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(jsonStr)
                    val body = json.optString("body", "")
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var remoteVersion = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk") && name.contains("_v")) {
                                remoteVersion = name.substringAfter("_v").substringBefore(".apk")
                                downloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    if (remoteVersion.isNotEmpty() && downloadUrl.isNotEmpty()) {
                        if (isNewVersion(currentVersionName, remoteVersion)) {
                            _updateState.value = UpdateUiState.HasUpdate(
                                remoteVersion = remoteVersion,
                                changelog = body,
                                downloadUrl = downloadUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "检测版本更新失败", e)
            }
        }
    }

    fun downloadAndInstall(context: Context, downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _updateState.value = UpdateUiState.Downloading(0f)

                val url = java.net.URL(downloadUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val fileLength = connection.contentLength
                val apkFile = java.io.File(context.getExternalFilesDir(null), "BIKA_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (fileLength > 0) {
                                val progress = totalBytesRead.toFloat() / fileLength.toFloat()
                                _updateState.value = UpdateUiState.Downloading(progress)
                            }
                        }
                    }
                }

                _updateState.value = UpdateUiState.Success(apkFile)
                installApk(context, apkFile)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "下载最新版安装包失败", e)
                _updateState.value = UpdateUiState.Error("下载更新包失败: ${e.localizedMessage ?: "网络异常"}")
            }
        }
    }

    private fun installApk(context: Context, file: java.io.File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "调用安装器失败", e)
            _updateState.value = UpdateUiState.Error("拉起系统安装器失败，请前往手机文件管理器安装此更新包")
        }
    }

    private fun isNewVersion(local: String, remote: String): Boolean {
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until size) {
            val lVal = localParts.getOrNull(i) ?: 0
            val rVal = remoteParts.getOrNull(i) ?: 0
            if (rVal > lVal) return true
            if (lVal > rVal) return false
        }
        return false
    }

    fun updateProfileSlogan(slogan: String) {
        viewModelScope.launch {
            try {
                network.updateUserProfileSlogan(slogan)
                restart()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "更新自我介绍失败", e)
            }
        }
    }
}