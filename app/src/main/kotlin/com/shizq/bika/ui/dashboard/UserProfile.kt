package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag

// ─────────────────────────────────────────────
// Domain model
// ─────────────────────────────────────────────

data class User(
    val name: String,
    val avatarUrl: String,
    val characters: List<String>,
    val level: Int,
    val exp: Int,
    val title: String,
    val gender: String,
    val slogan: String,
    val hasCheckedIn: Boolean,
) {
    val levelDisplay get() = "Lv.$level"
}

// ─────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────

sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
    data class Success(val user: User, val isOfflineCache: Boolean = false) : UserProfileUiState
}

/** 一次性操作（改签名、改密码等）的结果 */
sealed interface OperationResult {
    data object Success : OperationResult
    data class Error(val message: String) : OperationResult
}

sealed interface CheckInResult {
    val message: String

    data class Success(override val message: String) : CheckInResult
    data class Error(override val message: String) : CheckInResult
}

/**
 * 整个 Dashboard 的单一状态树。
 *
 * [lastReadHistory] / [activeChannels] / [favoriteTags] 原先是 ViewModel 上三个独立的
 * StateFlow，UI 要 collect 四处再自行拼装；favoriteTags 更是读走 ViewModel、写走
 * StateMachine，同一份数据两个 owner。现全部并入本状态树，由 StateMachine 单独持有。
 */
data class DashboardState(
    val userProfile: UserProfileUiState = UserProfileUiState.Loading,
    val lastReadHistory: DetailedReadingHistory? = null,
    val activeChannels: List<Channel> = emptyList(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val checkInResult: CheckInResult? = null,
)

// ─────────────────────────────────────────────
// Actions
// ─────────────────────────────────────────────

sealed interface DashboardAction {
    // 打卡
    data object CheckIn : DashboardAction

    /** 由 Screen 在 profile 首次加载成功后 dispatch；检查逻辑在 SM 内部完成 */
    data object AutoCheckIn : DashboardAction
    data object DismissCheckInResult : DashboardAction

    // 收藏标签
    data class AddFavoriteTag(val tag: FavoriteTag) : DashboardAction
    data class RemoveFavoriteTag(val tag: FavoriteTag) : DashboardAction
    data class UpdateFavoriteTagName(val tag: FavoriteTag, val newName: String) : DashboardAction
    data class MoveFavoriteTag(val fromIndex: Int, val toIndex: Int) : DashboardAction
    data class AddCustomFavoriteTag(val name: String) : DashboardAction
}
