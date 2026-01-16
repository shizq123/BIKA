package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendationData(
    val comics: List<RecommendComicDto>
)

@Serializable
data class RecommendComicDto(
    @SerialName("_id") val id: String,
    val title: String,
    val author: String = "",
    val pagesCount: Int = 0,
    val epsCount: Int = 0,
    val finished: Boolean = false,
    val categories: List<String> = emptyList(),
    val thumb: Media,
    val likesCount: Int = 0
)