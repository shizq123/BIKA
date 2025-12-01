package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeywordsResponse(
    @SerialName("keywords")
    val keywords: List<String>
)