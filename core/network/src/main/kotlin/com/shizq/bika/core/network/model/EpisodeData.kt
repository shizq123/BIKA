package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeData(
    val eps: EpisodePagination
)

@Serializable
data class EpisodePagination(
    val docs: List<Episode>,
    val total: Int,
    val limit: Int,
    val page: Int,
    val pages: Int
)

@Serializable
data class Episode(
    @SerialName("_id") val id: String,
    val title: String,
    val order: Int,
    @SerialName("updated_at") val updatedAt: String
)