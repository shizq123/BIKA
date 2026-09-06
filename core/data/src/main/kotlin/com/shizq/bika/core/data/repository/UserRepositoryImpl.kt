package com.shizq.bika.core.data.repository

import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserProfileSnapshotDataSource
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.UserProfile
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class UserRepositoryImpl @Inject constructor(
    private val network: BikaDataSource,
    private val userProfileSnapshotDataSource: UserProfileSnapshotDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
) : UserRepository {

    override suspend fun fetchUserProfile(): UserProfile {
        val profile = network.fetchUserProfile().user
        userProfileSnapshotDataSource.store(profile.toSnapshot())
        return profile
    }

    /**
     * name 为空视为"没有可用缓存"而非"缓存了一个匿名用户"：
     * 默认快照是全空字段，直接返回会让 UI 渲染一张空白资料卡。
     */
    override suspend fun getUserProfileSnapshot(): UserProfileSnapshot? =
        userProfileSnapshotDataSource.profile.first().takeIf { it.name.isNotEmpty() }

    override suspend fun clearCachedUserProfile() {
        userProfileSnapshotDataSource.clear()
    }

    override suspend fun punchIn() {
        network.punchIn()
    }

    override suspend fun updateSlogan(slogan: String) {
        network.updateUserProfileSlogan(slogan)
    }

    /**
     * 修改密码，成功后同步本地已保存的密码。
     *
     * 本地密码用于登录页预填。改密后若不同步，预填的是旧密码，
     * 用户下次登录会拿一个必然失败的密码去提交，且很难意识到原因。
     *
     * 仅在本地原本存有密码时才写入：密码为 null 表示用户当初没勾"记住密码"，
     * 这里不应替他做出保存密码的决定。
     */
    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        network.changePassword(oldPassword, newPassword)
        val hadStoredPassword = userCredentialsDataSource.userData.firstOrNull()?.password != null
        if (hadStoredPassword) {
            userCredentialsDataSource.setPassword(newPassword)
        }
    }
}

/**
 * 网络模型到本地快照的映射。
 *
 * 原先是在调用处摊成八个位置参数再于 DataSource 内组装回来，
 * 相邻的同类型参数（title/gender/slogan）写串了编译器不会报错。
 */
private fun UserProfile.toSnapshot() = UserProfileSnapshot(
    name = name,
    avatarUrl = imageUrl,
    level = level,
    exp = exp,
    title = title,
    gender = gender,
    slogan = slogan,
    honorBadges = characters,
)
