package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EyeCareConfig(
    val enabled: Boolean = false,
    val darkness: Float = 0.3f,
)