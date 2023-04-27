package com.shizq.bika.bean

data class ChatRoomProfileBean(
    val error: String,
    val message: String,
    val statusCode: Int,
    val profile: Profile
) {
    data class Profile(
        val avatarUrl: String,
        val birthday: String,
        val characters: List<Any>,
        val created_at: String,
        val email: String,
        val exp: Int,
        val gender: String,
        val id: String,
        val level: Int,
        val name: String,
        val role: String,
        val slogan: String,
        val title: String
    )
}