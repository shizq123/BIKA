@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.dashboard

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow


private const val TAG = "DashboardSM"

class DashboardStateMachine @Inject constructor(
    private val network: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : FlowReduxStateMachineFactory<DashboardState, DashboardAction>() {

    private val profileRestarter = FlowRestarter()

    init {
        initializeWith { DashboardState() }
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
                                        gender = prefs.cachedUserGender,
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
}
