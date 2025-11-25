package com.shizq.bika.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ProfileBean(
    val user: User
) {
    data class User(
        val _id: String,
        val avatar: Avatar,
        val birthday: String,
        val character: String,
        val characters: List<Any>,
        val created_at: String,
        val email: String,
        val exp: Int,
        val gender: String,
        val isPunched: Boolean,
        val level: Int,
        val name: String,
        val slogan: String,
        val title: String,
        val verified: Boolean
    ) {
        data class Avatar(
            val fileServer: String,
            val originalName: String,
            val path: String
        )
    }
}

@Serializable
data class UserProfile(
    @SerialName("user")
    val user: User = User()
) {
    @Serializable
    data class User(
        @SerialName("avatar")
        val avatar: Media = Media(),
        @SerialName("birthday")
        val birthday: String = "",
        @SerialName("character")
        val character: String = "",
        @SerialName("characters")
        val characters: List<String> = listOf(),
        @SerialName("created_at")
        val createdAt: String = "",
        @SerialName("email")
        val email: String = "",
        @SerialName("exp")
        val exp: Int = 0,
        @SerialName("gender")
        val gender: String = "",
        @SerialName("_id")
        val id: String = "",
        @SerialName("isPunched")
        val isPunched: Boolean = false,
        @SerialName("level")
        val level: Int = 0,
        @SerialName("name")
        val name: String = "",
        @SerialName("slogan")
        val slogan: String = "",
        @SerialName("title")
        val title: String = "",
        @SerialName("verified")
        val verified: Boolean = false
    )
}