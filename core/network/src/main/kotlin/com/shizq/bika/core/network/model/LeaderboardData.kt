package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardData(
    val comics: List<ComicSimple>
)

@Serializable
data class KnightLeaderboardData(
    @SerialName("users")
    val users: List<UserData>
)