package com.shizq.bika.core.data.repository

import com.shizq.bika.core.network.model.UserProfile

interface UserRepository {
    /**
     * 获取用户 profile（网络）。
     * 成功时自动将结果写入本地缓存。
     */
    suspend fun fetchUserProfile(): UserProfile

    /** 打卡 */
    suspend fun punchIn()

    /** 更新个性签名 */
    suspend fun updateSlogan(slogan: String)

    /** 修改密码 */
    suspend fun changePassword(oldPassword: String, newPassword: String)
}
