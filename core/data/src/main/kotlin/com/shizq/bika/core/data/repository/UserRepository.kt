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
     */
    suspend fun getUserProfileSnapshot(): UserProfileSnapshot?

    /**
     * 清空本地资料缓存。登出时调用，避免下一个账号在联网拿到自己的资料前，
     * 先看到上一个账号的资料卡。
     */
    suspend fun clearCachedUserProfile()

    /** 打卡 */
    suspend fun punchIn()

    /** 更新个性签名 */
    suspend fun updateSlogan(slogan: String)

    /** 修改密码 */
    suspend fun changePassword(oldPassword: String, newPassword: String)
}
