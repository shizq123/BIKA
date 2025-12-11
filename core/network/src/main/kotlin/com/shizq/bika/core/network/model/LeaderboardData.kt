package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardData(
    val comics: List<ComicDto>
)