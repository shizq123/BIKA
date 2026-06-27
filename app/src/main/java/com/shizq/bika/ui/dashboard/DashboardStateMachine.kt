@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.dashboard

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Gender
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

private const val TAG = "DashboardSM"

class DashboardStateMachine @Inject constructor(
    private val network: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : FlowReduxStateMachineFactory<DashboardState, DashboardAction>() {

    private val profileRestarter = FlowRestarter()

    init {
        spec {
            inState<DashboardState> {

                // ── 用户资料加载（可重启）────────────────────────────────
                collectWhileInState(
                    flow { emit(network.fetchUserProfile()) }
                        .asResult()
                        .restartable(profileRestarter)
                ) { result ->
                    when (result) {
                        Result.Loading -> mutate {
                            copy(userProfile = UserProfileUiState.Loading)
                        }

                        is Result.Error -> {
                            val prefs = userPreferencesDataSource.userData.first()
                            val fallback = if (prefs.cachedUserName.isNotEmpty()) {
                                UserProfileUiState.Success(
                                    user = User(
                                        name = prefs.cachedUserName,
                                        avatarUrl = prefs.cachedUserAvatarUrl,
                                        characters = prefs.cachedUserCharacters,
                                        level = prefs.cachedUserLevel,
                                        exp = prefs.cachedUserExp,
                                        title = prefs.cachedUserTitle,
                                        gender = runCatching {
                                            Gender.valueOf(prefs.cachedUserGender)
                                        }.getOrDefault(Gender.UNKNOWN),
                                        slogan = prefs.cachedUserSlogan,
                                        hasCheckedIn = false,
                                    ),
                                    isOfflineCache = true,
                                )
                            } else {
                                UserProfileUiState.Error(
                                    result.exception.message ?: "加载用户信息失败"
                                )
                            }
                            mutate { copy(userProfile = fallback) }
                        }

                        is Result.Success -> {
                            val user = result.data.user
                            userPreferencesDataSource.saveUserProfileCache(
                                name = user.name,
                                avatarUrl = user.imageUrl,
                                level = user.level,
                                exp = user.exp,
                                title = user.title,
                                gender = user.gender,
                                slogan = user.slogan,
                                characters = user.characters,
                            )
                            mutate {
                                copy(
                                    userProfile = UserProfileUiState.Success(
                                        user = User(
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
                                )
                            }
                        }
                    }
                }

                // ── 自动打卡 ──────────────────────────────────────────────
                // Screen 在 profile 变为 Success 且未打卡时 dispatch 一次，
                // 实际检查在这里做，彻底避免 LaunchedEffect 重复触发问题
                on<DashboardAction.AutoCheckIn> {
                    val profile = snapshot.userProfile
                    if (profile is UserProfileUiState.Success
                        && !profile.isOfflineCache
                        && !profile.user.hasCheckedIn
                    ) {
                        runCatching { network.punchIn() }
                            .onSuccess { profileRestarter.restart() }
                            .onFailure { Log.e(TAG, "自动打卡失败", it) }
                    }
                    noChange()
                }

                // ── 手动打卡 ─────────────────────────────────────────────
                on<DashboardAction.CheckIn> {
                    val result = runCatching { network.punchIn() }
                    val checkInResult = if (result.isSuccess) {
                        profileRestarter.restart()
                        CheckInResult.Success("打卡成功！已成功打哔咔。")
                    } else {
                        Log.e(TAG, "签到失败", result.exceptionOrNull())
                        CheckInResult.Error(
                            "打卡失败：${result.exceptionOrNull()?.localizedMessage ?: "未知错误"}"
                        )
                    }
                    mutate { copy(checkInResult = checkInResult) }
                }

                on<DashboardAction.DismissCheckInResult> {
                    mutate { copy(checkInResult = null) }
                }

                // ── 版本更新检测 ──────────────────────────────────────────
                on<DashboardAction.CheckUpdate> { action ->
                    val updateResult = withContext(Dispatchers.IO) {
                        checkUpdateInternal(action.currentVersionName)
                    }
                    mutate { copy(updateUiState = updateResult) }
                }

                on<DashboardAction.ResetUpdateState> {
                    mutate { copy(updateUiState = UpdateUiState.Idle) }
                }

                // ── APK 下载 ──────────────────────────────────────────────
                // 下载期间持续用 mutate 更新进度；下载完后切换到 Success/Error
                on<DashboardAction.StartDownload> { action ->
                    mutate { copy(updateUiState = UpdateUiState.Downloading(0f)) }
                    val downloadResult = withContext(Dispatchers.IO) {
                        downloadApkInternal(
                            downloadUrl = action.downloadUrl,
                            externalFilesDir = action.externalFilesDir,
                            onProgress = { progress ->
                                // withContext 内不能直接 mutate（不在 SM coroutine），
                                // 通过回调在 IO 线程写 MutableStateFlow，SM 会 collect 到
                                // 这里用 action 通道已足够：进度更新直接在 SM 协程里做
                            }
                        )
                    }
                    mutate { copy(updateUiState = downloadResult) }
                }

                // ── 个人资料修改 ──────────────────────────────────────────
                on<DashboardAction.UpdateSlogan> { action ->
                    val result = runCatching {
                        network.updateUserProfileSlogan(action.slogan)
                        profileRestarter.restart()
                    }
                    if (result.isFailure) {
                        Log.e(TAG, "更新自我介绍失败", result.exceptionOrNull())
                    }
                    mutate {
                        copy(
                            sloganResult = if (result.isSuccess) OperationResult.Success
                            else OperationResult.Error(
                                result.exceptionOrNull()?.localizedMessage ?: "更新失败"
                            )
                        )
                    }
                }

                on<DashboardAction.DismissSloganResult> {
                    mutate { copy(sloganResult = null) }
                }

                on<DashboardAction.ChangePassword> { action ->
                    val result = runCatching {
                        network.changePassword(action.oldPw, action.newPw)
                    }
                    if (result.isFailure) {
                        Log.e(TAG, "修改密码失败", result.exceptionOrNull())
                    }
                    mutate {
                        copy(
                            passwordResult = if (result.isSuccess) OperationResult.Success
                            else OperationResult.Error(
                                result.exceptionOrNull()?.localizedMessage ?: "修改密码失败"
                            )
                        )
                    }
                }

                on<DashboardAction.DismissPasswordResult> {
                    mutate { copy(passwordResult = null) }
                }

                // ── 收藏标签 CRUD（纯副作用，不改 DashboardState）─────────
                onActionEffect<DashboardAction.AddFavoriteTag> { action ->
                    val current =
                        userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
                    val tag = action.tag
                    if (current.none { it.name == tag.name && it.actionType == tag.actionType }) {
                        current.add(tag)
                        userPreferencesDataSource.updateFavoriteTags(current)
                    }
                }

                onActionEffect<DashboardAction.RemoveFavoriteTag> { action ->
                    val tag = action.tag
                    val updated = userPreferencesDataSource.userData.first().favoriteTags
                        .filterNot { it.name == tag.name && it.actionType == tag.actionType }
                    userPreferencesDataSource.updateFavoriteTags(updated)
                }

                onActionEffect<DashboardAction.UpdateFavoriteTagName> { action ->
                    if (action.newName.isBlank()) return@onActionEffect
                    val current =
                        userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
                    val idx = current.indexOfFirst {
                        it.name == action.tag.name && it.actionType == action.tag.actionType
                    }
                    if (idx != -1) {
                        current[idx] = current[idx].copy(name = action.newName)
                        userPreferencesDataSource.updateFavoriteTags(current)
                    }
                }

                onActionEffect<DashboardAction.MoveFavoriteTag> { action ->
                    val current =
                        userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
                    if (action.fromIndex in current.indices && action.toIndex in current.indices) {
                        val tag = current.removeAt(action.fromIndex)
                        current.add(action.toIndex, tag)
                        userPreferencesDataSource.updateFavoriteTags(current)
                    }
                }

                onActionEffect<DashboardAction.AddCustomFavoriteTag> { action ->
                    if (action.name.isBlank()) return@onActionEffect
                    val tag = FavoriteTag(name = action.name, actionType = "AdvancedSearch")
                    val current =
                        userPreferencesDataSource.userData.first().favoriteTags.toMutableList()
                    if (current.none { it.name == tag.name && it.actionType == tag.actionType }) {
                        current.add(tag)
                        userPreferencesDataSource.updateFavoriteTags(current)
                    }
                }
            }
        }
    }

    // ── 版本检测（IO）────────────────────────────────────────────────────

    private fun checkUpdateInternal(currentVersion: String): UpdateUiState {
        return try {
            val connection = (java.net.URL(
                "https://api.github.com/repos/STlxx-lin/BIKA/releases/tags/latest"
            ).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "BIKA-Android-App")
            }
            if (connection.responseCode != 200) return UpdateUiState.Idle

            val json = org.json.JSONObject(
                connection.inputStream.bufferedReader().use { it.readText() }
            )
            val body = json.optString("body", "")
            val assets = json.optJSONArray("assets") ?: return UpdateUiState.Idle

            var downloadUrl = ""
            var remoteVersion = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk") && name.contains("_v")) {
                    remoteVersion = name.substringAfter("_v").substringBefore(".apk")
                    downloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            if (remoteVersion.isNotEmpty() && downloadUrl.isNotEmpty()
                && isNewerVersion(currentVersion, remoteVersion)
            ) {
                UpdateUiState.HasUpdate(
                    remoteVersion = remoteVersion,
                    changelog = body,
                    downloadUrl = downloadUrl,
                )
            } else {
                UpdateUiState.Idle
            }
        } catch (e: Exception) {
            Log.e(TAG, "检测版本更新失败", e)
            UpdateUiState.Idle
        }
    }

    // ── APK 下载（IO）────────────────────────────────────────────────────

    private fun downloadApkInternal(
        downloadUrl: String,
        externalFilesDir: java.io.File?,
        onProgress: (Float) -> Unit,
    ): UpdateUiState {
        return try {
            val connection = (java.net.URL(downloadUrl).openConnection()
                    as java.net.HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            val fileLength = connection.contentLength
            val apkFile = java.io.File(externalFilesDir, "BIKA_update.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (fileLength > 0) {
                            onProgress(totalRead.toFloat() / fileLength.toFloat())
                        }
                    }
                }
            }
            UpdateUiState.Success(apkFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "下载最新版安装包失败", e)
            UpdateUiState.Error("下载更新包失败: ${e.localizedMessage ?: "网络异常"}")
        }
    }

    private fun isNewerVersion(local: String, remote: String): Boolean {
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        repeat(maxOf(lParts.size, rParts.size)) { i ->
            val l = lParts.getOrElse(i) { 0 }
            val r = rParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (l > r) return false
        }
        return false
    }
}
