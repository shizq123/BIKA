package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val autoCheckIn: Boolean = true,
    val secureScreenEnabled: Boolean = false,
    val usePredictiveBack: Boolean = false,
    val fontScale: Float = 1.0f,
)