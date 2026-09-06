@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.dashboard

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable
import com.shizq.bika.core.data.repository.DashboardRepository
import com.shizq.bika.core.data.repository.UserRepository
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.network.model.UserProfile
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.ui.feed.FeedActionType
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

private val logger = KotlinLogging.logger("DashboardSM")

class DashboardStateMachine @Inject constructor(
    private val userRepository: UserRepository,
    private val dashboardRepository: DashboardRepository,
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

                // ── 本地数据源投射进状态树 ────────────────────────────────
                // 这三条原先是 ViewModel 上独立的 stateIn flow，UI 需要 collect
                // 四处再自行拼装。收进状态树后 UI 只有一个订阅点，且 favoriteTags
                // 的读与写（见下方 CRUD）归到同一个 owner。
                collectWhileInState(dashboardRepository.lastReadHistory) { history ->
                    mutate { copy(lastReadHistory = history) }
                }

                collectWhileInState(dashboardRepository.activeChannels) { channels ->
                    mutate { copy(activeChannels = channels) }
                }

                collectWhileInState(dashboardRepository.favoriteTags) { tags ->
                    mutate { copy(favoriteTags = tags) }
                }

                // ── 用户资料加载（可重启）────────────────────────────────
                // 仓储负责写本地缓存，这里只做 network model → UI model 的映射
                collectWhileInState(
                    flow { emit(userRepository.fetchUserProfile()) }
                        .asResult()
                        .restartable(profileRestarter)
                ) { result ->
                    when (result) {
                        Result.Loading -> mutate {
                            copy(userProfile = UserProfileUiState.Loading)
                        }

                        is Result.Error -> {
                            // 「缓存是否可用」的判定在仓储里（name 非空），这里只消费结果
                            val fallback = userRepository.getUserProfileSnapshot()
                                ?.let {
                                    UserProfileUiState.Success(
                                        it.toUser(),
                                        isOfflineCache = true
                                    )
                                }
                                ?: UserProfileUiState.Error(
                                    result.exception.message ?: "加载用户信息失败"
                                )
                            mutate { copy(userProfile = fallback) }
                        }

                        is Result.Success -> mutate {
                            copy(userProfile = UserProfileUiState.Success(result.data.toUser()))
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
                        runCatching { userRepository.punchIn() }
                            .onSuccess { profileRestarter.restart() }
                            .onFailure { logger.error(it) { "自动打卡失败" } }
                    }
                    noChange()
                }

                // ── 手动打卡 ─────────────────────────────────────────────
                on<DashboardAction.CheckIn> {
                    val result = runCatching { userRepository.punchIn() }
                    val checkInResult = if (result.isSuccess) {
                        profileRestarter.restart()
                        CheckInResult.Success("打卡成功！已成功打哔咔。")
                    } else {
                        logger.error(result.exceptionOrNull()) { "签到失败" }
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
                        runCatching { userRepository.updateSlogan(action.slogan) }
                    } finally {
                        // finally 而非顺序赋值：handler 被取消时也要清掉标志，
                        // 否则 submitting 停在 true，对话框永久禁用。
                        submitting.value = false
                    }
                    result.onSuccess { profileRestarter.restart() }
                    if (result.isFailure) {
                        logger.error(result.exceptionOrNull()) { "更新自我介绍失败" }
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
                            userRepository.changePassword(action.oldPassword, action.newPassword)
                        }
                    } finally {
                        submitting.value = false
                    }
                    if (result.isFailure) {
                        logger.error(result.exceptionOrNull()) { "修改密码失败" }
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
                    dashboardRepository.updateFavoriteTags { addFavoriteTag(it, action.tag) }
                }

                onActionEffect<DashboardAction.RemoveFavoriteTag> { action ->
                    dashboardRepository.updateFavoriteTags { removeFavoriteTag(it, action.tag) }
                }

                onActionEffect<DashboardAction.UpdateFavoriteTagName> { action ->
                    // 这里的空名短路是为了省掉一次无谓的 DataStore 事务；
                    // renameFavoriteTag 内部同样有守卫，两者都不能删——
                    // 事务内的那道才是真正保证不写入空名的。
                    if (action.newName.isBlank()) return@onActionEffect
                    dashboardRepository.updateFavoriteTags {
                        renameFavoriteTag(it, action.tag, action.newName)
                    }
                }

                onActionEffect<DashboardAction.MoveFavoriteTag> { action ->
                    dashboardRepository.updateFavoriteTags {
                        moveFavoriteTag(it, action.fromIndex, action.toIndex)
                    }
                }

                onActionEffect<DashboardAction.AddCustomFavoriteTag> { action ->
                    if (action.name.isBlank()) return@onActionEffect
                    val tag = FavoriteTag(
                        name = action.name,
                        actionType = FeedActionType.AdvancedSearch.storageValue,
                    )
                    dashboardRepository.updateFavoriteTags { addFavoriteTag(it, tag) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 数据源模型 → UI 模型
// ─────────────────────────────────────────────

private fun UserProfile.toUser() = User(
    name = name,
    avatarUrl = imageUrl,
    characters = characters,
    level = level,
    exp = exp,
    title = title,
    gender = gender,
    slogan = slogan,
    hasCheckedIn = isPunched,
)

/**
 * 快照里没有 isPunched：打卡状态是当天的，缓存下来必然过时。
 * 这里硬编码 false，配合 [UserProfileUiState.Success.isOfflineCache] 让 AutoCheckIn
 * 在离线态直接短路——不会拿一个陈旧的「未打卡」去触发打卡请求。
 */
private fun UserProfileSnapshot.toUser() = User(
    name = name,
    avatarUrl = avatarUrl,
    characters = honorBadges,
    level = level,
    exp = exp,
    title = title,
    gender = gender,
    slogan = slogan,
    hasCheckedIn = false,
)
