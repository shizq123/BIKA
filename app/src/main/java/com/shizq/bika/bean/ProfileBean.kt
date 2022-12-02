package com.shizq.bika.bean

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


