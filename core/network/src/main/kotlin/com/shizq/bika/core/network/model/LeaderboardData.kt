package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardData(
    val comics: List<ComicSummary>
)

@Serializable
data class KnightLeaderboardData(
    @SerialName("users")
    val users: List<UserData>
)