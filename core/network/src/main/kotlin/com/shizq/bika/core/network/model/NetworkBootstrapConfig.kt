package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBootstrapConfig(
    @SerialName("addresses")
    val addresses: List<String>,
)