package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val originalName: String = "",
    val path: String = "",
    val fileServer: String = "",
)