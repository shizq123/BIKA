package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

/** 本地留存的用户资料快照，供无网络时回退展示；可能过时。 */
@Serializable
data class UserProfileSnapshot(
    val name: String = "",
    val avatarUrl: String = "",
    val level: Int = 0,
    val exp: Int = 0,
    val title: String = "",
    val gender: String = "",
    val slogan: String = "",
    val honorBadges: List<String> = emptyList(),
)
