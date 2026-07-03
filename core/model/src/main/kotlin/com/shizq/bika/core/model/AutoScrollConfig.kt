package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AutoScrollConfig(
    val enabled: Boolean = false,
    val speed: Int = 3,
)