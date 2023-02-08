package com.shizq.bika.bean

data class ChatBlackListBean(
    val error: String,
    val message: String,
    val statusCode: Int,
    val docs: List<Doc>,
    var limit: Int = 0,
    val offset: Int,
    val total: Int
) {
    data class Doc(
        val id: String,
        val user: User
    ) {
        data class User(
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
}