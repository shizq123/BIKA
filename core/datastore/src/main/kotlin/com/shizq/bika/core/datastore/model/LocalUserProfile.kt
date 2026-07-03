package com.shizq.bika.core.datastore.model

import kotlinx.coroutines.flow.Flow

interface UserCacheDataSource {
    val profile: Flow<LocalUserProfile>
    suspend fun saveProfile(profile: LocalUserProfile)
}

data class LocalUserProfile(
    val name: String = "",
    val avatarUrl: String = "",
    val level: Int = 0,
    val exp: Int = 0,
    val title: String = "",
    val gender: String = "",
    val slogan: String = "",
    val characters: List<String> = emptyList(),
)