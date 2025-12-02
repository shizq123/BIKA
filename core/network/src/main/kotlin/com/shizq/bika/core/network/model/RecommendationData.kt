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
    val author: String,
    val pagesCount: Int,
    val epsCount: Int,
    val finished: Boolean,
    val categories: List<String>,
    val thumb: Media,
    val likesCount: Int
)