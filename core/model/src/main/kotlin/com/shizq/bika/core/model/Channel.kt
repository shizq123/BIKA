package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val displayName: String,
    val resName: String,
    val isActive: Boolean = true,
)
