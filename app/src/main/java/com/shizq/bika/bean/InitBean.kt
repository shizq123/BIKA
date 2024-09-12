package com.shizq.bika.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitBean(
    @SerialName("addresses")
    val addresses: List<String> = listOf(),
    @SerialName("status")
    val status: String = "",
)
