package com.shizq.bika.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag
import kotlinx.coroutines.flow.StateFlow
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Gender
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
    private val historyDao: ReadingHistoryDao,
) : ViewModel() {
    val lastReadHistory = historyDao.getDetailedHistories()
        .map { list -> list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
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
                is Result.Error -> {
                    // 网络失败：读取本地缓存的用户资料作为回退
                    val prefs = userPreferencesDataSource.userData.first()
                    if (prefs.cachedUserName.isNotEmpty()) {
                        UserProfileUiState.Success(
                            User(
                                name = prefs.cachedUserName,
                                avatarUrl = prefs.cachedUserAvatarUrl,
                                characters = prefs.cachedUserCharacters,
                                level = prefs.cachedUserLevel,
                                exp = prefs.cachedUserExp,
                                title = prefs.cachedUserTitle,
                                gender = try {
                                    Gender.valueOf(prefs.cachedUserGender)
                                } catch (e: Exception) {
                                    Gender.UNKNOWN
                                },
                                slogan = prefs.cachedUserSlogan,
                                hasCheckedIn = false,
                            ),
                            isOfflineCache = true
                        )
                    } else {
                        UserProfileUiState.Error(result.exception.message ?: "")
                    }
                }
                is Result.Success -> {
                    val user = result.data.user
                    // 网络成功：同步更新本地缓存
                    userPreferencesDataSource.saveUserProfileCache(
                        name = user.name,
                        avatarUrl = user.imageUrl,
                        level = user.level,
                        exp = user.exp,
                        title = user.title,
                        gender = user.gender.name,
                        slogan = user.slogan,
                        characters = user.characters,
                    )
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
            try {
                network.punchIn()
                if (!isAuto) {
                    _checkInEvent.emit(CheckInEvent.Success("打卡成功！已成功打哔咔。"))
                }
                restart()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "签到失败", e)
                if (!isAuto) {
                    _checkInEvent.emit(CheckInEvent.Error("打卡失败：${e.localizedMessage ?: "未知错误"}"))
                }
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

    fun updateProfileSlogan(
        slogan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                network.updateUserProfileSlogan(slogan)
                restart()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "更新自我介绍失败", e)
                onError(e.localizedMessage ?: "更新失败")
            }
        }
    }

    fun changePassword(
        oldPw: String,
        newPw: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                network.changePassword(oldPw, newPw)
                onSuccess()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "修改密码失败", e)
                onError(e.localizedMessage ?: "修改密码失败")
            }
        }
    }

    val favoriteTags: StateFlow<List<FavoriteTag>> = userPreferencesDataSource.userData
        .map { it.favoriteTags }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            val currentTags = userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
            if (currentTags.none { it.name == tag.name && it.actionType == tag.actionType }) {
                currentTags.add(tag)
                userPreferencesDataSource.updateFavoriteTags(currentTags)
            }
        }
    }

    fun removeFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            val currentTags = userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
            currentTags.removeAll { it.name == tag.name && it.actionType == tag.actionType }
            userPreferencesDataSource.updateFavoriteTags(currentTags)
        }
    }

    fun updateFavoriteTagName(tag: FavoriteTag, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val currentTags = userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
            val index = currentTags.indexOfFirst { it.name == tag.name && it.actionType == tag.actionType }
            if (index != -1) {
                currentTags[index] = currentTags[index].copy(name = newName)
                userPreferencesDataSource.updateFavoriteTags(currentTags)
            }
        }
    }

    fun moveFavoriteTag(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentTags = userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
            if (fromIndex in currentTags.indices && toIndex in currentTags.indices) {
                val tag = currentTags.removeAt(fromIndex)
                currentTags.add(toIndex, tag)
                userPreferencesDataSource.updateFavoriteTags(currentTags)
            }
        }
    }

    fun addCustomFavoriteTag(name: String) {
        if (name.isBlank()) return
        addFavoriteTag(FavoriteTag(name = name, actionType = "AdvancedSearch"))
    }
}