package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val token: String
)