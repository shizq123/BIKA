package com.shizq.bika.ui.main

data class UserProfile(
    val name: String = "未登录",
    val avatarUrl: String = "",
    val frameUrl: String = "",
    val level: Int = 1,
    val exp: Int = 0,
    val title: String = "萌新",
    val gender: String = "bot",
    val slogan: String = "",
    val isPunched: Boolean = false
)