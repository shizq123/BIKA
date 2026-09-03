package com.shizq.bika.core.data.repository

import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.UserProfile
import jakarta.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class UserRepositoryImpl @Inject constructor(
    private val network: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
) : UserRepository {

    override suspend fun fetchUserProfile(): UserProfile {
        val profile = network.fetchUserProfile().user
        // 网络成功时同步写入本地缓存，移出 ViewModel 的 Flow.map 副作用
        userPreferencesDataSource.saveUserProfileCache(
            name = profile.name,
            avatarUrl = profile.imageUrl,
            level = profile.level,
            exp = profile.exp,
            title = profile.title,
            gender = profile.gender,
            slogan = profile.slogan,
            honorBadges = profile.characters,
        )
        return profile
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
