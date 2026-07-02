package com.shizq.bika.core.model.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val autoCheckIn: Boolean = true,
    val secureScreenEnabled: Boolean = false,
    @SerialName("usePredictiveBack")
    val predictiveBackEnabled: Boolean = false,
    val fontScale: Float = 1.0f,
)
