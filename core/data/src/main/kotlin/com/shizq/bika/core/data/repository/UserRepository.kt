package com.shizq.bika.core.data.repository

import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.network.model.UserProfile

interface UserRepository {
    /**
     * 获取用户 profile（网络）。
     * 成功时自动将结果写入本地缓存。
     */
    suspend fun fetchUserProfile(): UserProfile

    /**
     * 读取本地缓存的用户资料，供无网时回退展示；从未缓存过时返回 null。
     *
     * 与 [fetchUserProfile] 内部的写缓存成对：原先写在仓储、读在 StateMachine
     * 直连 DataStore，缓存的存在形式泄漏到了 UI 层。
     */
    suspend fun cachedUserProfile(): UserProfileSnapshot?

    /** 打卡 */
    suspend fun punchIn()

    /** 更新个性签名 */
    suspend fun updateSlogan(slogan: String)

    /** 修改密码 */
    suspend fun changePassword(oldPassword: String, newPassword: String)
}
