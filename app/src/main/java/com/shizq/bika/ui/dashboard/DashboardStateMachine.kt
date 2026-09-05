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
import com.shizq.bika.ui.feed.FeedActionType
import com.shizq.bika.ui.feed.isSameTag
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow


private const val TAG = "DashboardSM"

class DashboardStateMachine @Inject constructor(
    private val network: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : FlowReduxStateMachineFactory<DashboardState, DashboardAction>() {

    private val profileRestarter = FlowRestarter()

    /**
     * 一次性写操作的提交中标志。
     *
     * `on<>` handler 只能返回一个 ChangedState，无法在同一个 handler 里先发
     * `isSubmitting = true` 再发结果。这里沿用 UpdateStateMachine.downloadProgress
     * 的做法，用一个外部 flow 承载中间态，由 collectWhileInState 投射进 state。
     */
    private val submitting = MutableStateFlow(false)

    init {
        initializeWith { DashboardState() }
        spec {
            inState<DashboardState> {

                collectWhileInState(submitting) { inFlight ->
                    mutate { copy(isSubmitting = inFlight) }
                }

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
                            val fallback = if (prefs.profile.name.isNotEmpty()) {
                                UserProfileUiState.Success(
                                    user = User(
                                        name = prefs.profile.name,
                                        avatarUrl = prefs.profile.avatarUrl,
                                        characters = prefs.profile.honorBadges,
                                        level = prefs.profile.level,
                                        exp = prefs.profile.exp,
                                        title = prefs.profile.title,
                                        gender = prefs.profile.gender,
                                        slogan = prefs.profile.slogan,
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
                                honorBadges = user.characters,
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
                    // runCatching 只包住网络调用：restart 只是刷新本地 profile 流，
                    // 它的失败不代表签名没改成功，混进来会让用户看到「更新失败」
                    // 而服务端其实已经生效。
                    val result = try {
                        submitting.value = true
                        runCatching { network.updateUserProfileSlogan(action.slogan) }
                    } finally {
                        // finally 而非顺序赋值：handler 被取消时也要清掉标志，
                        // 否则 submitting 停在 true，对话框永久禁用。
                        submitting.value = false
                    }
                    result.onSuccess { profileRestarter.restart() }
                    if (result.isFailure) {
                        Log.e(TAG, "更新自我介绍失败", result.exceptionOrNull())
                    }
                    mutate {
                        copy(
                            sloganResult = if (result.isSuccess) OperationResult.Success
                            else OperationResult.Error(
                                result.exceptionOrNull()?.localizedMessage ?: "更新失败"
                            ),
                            // 显式置 false，不依赖本次 mutate 与 collectWhileInState
                            // 那次 mutate 的先后顺序——若本次基于更早的快照，
                            // 只靠 submitting 流会把 true 写回来。
                            isSubmitting = false,
                        )
                    }
                }

                on<DashboardAction.DismissSloganResult> {
                    mutate { copy(sloganResult = null) }
                }

                on<DashboardAction.ChangePassword> { action ->
                    val result = try {
                        submitting.value = true
                        runCatching {
                            network.changePassword(action.oldPassword, action.newPassword)
                        }
                    } finally {
                        submitting.value = false
                    }
                    if (result.isFailure) {
                        Log.e(TAG, "修改密码失败", result.exceptionOrNull())
                    }
                    mutate {
                        copy(
                            passwordResult = if (result.isSuccess) OperationResult.Success
                            else OperationResult.Error(
                                result.exceptionOrNull()?.localizedMessage ?: "修改密码失败"
                            ),
                            isSubmitting = false,
                        )
                    }
                }

                on<DashboardAction.DismissPasswordResult> {
                    mutate { copy(passwordResult = null) }
                }

                // ── 收藏标签 CRUD（纯副作用，不改 DashboardState）─────────
                // 统一走 updateFavoriteTags(transform) 这个原子重载：transform 在 DataStore
                // 事务内执行，快速连续点击不会因「先读快照再整表写回」而丢更新。
                // 身份判定复用 FavoriteTag.isSameTag，避免 name + actionType 的谓词散落多处。
                onActionEffect<DashboardAction.AddFavoriteTag> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        if (tags.any { it.isSameTag(action.tag) }) tags else tags + action.tag
                    }
                }

                onActionEffect<DashboardAction.RemoveFavoriteTag> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        tags.filterNot { it.isSameTag(action.tag) }
                    }
                }

                onActionEffect<DashboardAction.UpdateFavoriteTagName> { action ->
                    if (action.newName.isBlank()) return@onActionEffect
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        tags.map {
                            if (it.isSameTag(action.tag)) it.copy(name = action.newName) else it
                        }
                    }
                }

                onActionEffect<DashboardAction.MoveFavoriteTag> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        if (action.fromIndex !in tags.indices || action.toIndex !in tags.indices) {
                            tags
                        } else {
                            tags.toMutableList()
                                .apply { add(action.toIndex, removeAt(action.fromIndex)) }
                        }
                    }
                }

                onActionEffect<DashboardAction.AddCustomFavoriteTag> { action ->
                    if (action.name.isBlank()) return@onActionEffect
                    val tag = FavoriteTag(
                        name = action.name,
                        actionType = FeedActionType.AdvancedSearch.storageValue,
                    )
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        if (tags.any { it.isSameTag(tag) }) tags else tags + tag
                    }
                }
            }
        }
    }
}
