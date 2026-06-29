package com.shizq.bika.core.data.repository

import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.UserProfile
import jakarta.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val network: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
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
            characters = profile.characters,
        )
        return profile
    }

    override suspend fun punchIn() {
        network.punchIn()
    }

    override suspend fun updateSlogan(slogan: String) {
        network.updateUserProfileSlogan(slogan)
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        network.changePassword(oldPassword, newPassword)
    }
}
