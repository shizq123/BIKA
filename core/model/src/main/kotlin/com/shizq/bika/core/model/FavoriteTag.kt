package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteTag(
    val name: String,
    val actionType: String,
    val actionId: String = ""
)