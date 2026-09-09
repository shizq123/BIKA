package com.shizq.bika.core.data.repository

import com.shizq.bika.core.data.model.UserProfileState
import com.shizq.bika.core.result.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * 用户资料，网络为准、本地缓存兜底。
     *
     * 这是 profile 的唯一订阅点，也是 profile 失效的唯一通道 —— 想让资料卡刷新，
     * 调 [refreshUserProfile]，不要持有别人的 FlowRestarter。原先重启器是
     * DashboardStateMachine 的一个 private 字段，导致任何想改资料的第三方
     * （修改签名对话框）都必须长在那个状态机里才能触发刷新。
     *
     * 多个订阅者共享同一次网络请求；无订阅者后保留 5 秒，页面重建不重复发请求。
     */
    val userProfile: Flow<Result<UserProfileState>>

    /** 触发 [userProfile] 重新取数。写操作成功后调用。 */
    suspend fun refreshUserProfile()

    /**
     * 清空本地资料缓存。登出时调用，避免下一个账号在联网拿到自己的资料前，
     * 先看到上一个账号的资料卡。
     */
    suspend fun clearUserProfileSnapshot()

    /** 打卡 */
    suspend fun punchIn()

    /** 更新个性签名 */
    suspend fun updateSlogan(slogan: String)

    /** 修改密码 */
    suspend fun changePassword(oldPassword: String, newPassword: String)
}
