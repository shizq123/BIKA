package com.shizq.bika.bean

import kotlinx.serialization.Serializable

data class SignInBean(
    val token: String,
    val question1: String,
    val question2: String,
    val question3: String,
    val password: String
)

@Serializable
data class LoginData(
    val token: String
)
