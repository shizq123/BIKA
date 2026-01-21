package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val token: String
)

@Serializable
data class SignInData(
    val token: String = "",
    val question1: String = "",
    val question2: String = "",
    val question3: String = "",
    val password: String = ""
)