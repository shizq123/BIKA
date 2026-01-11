package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object DashboardNavKey : NavKey

@Serializable
sealed interface FeedNavKey : NavKey {
    object Collection : FeedNavKey
    object Random : FeedNavKey
    data class Topic(val name: String) : FeedNavKey
    object Recent : FeedNavKey
    data class Knight(val name: String, val id: String) : FeedNavKey
}

@Serializable
object LeaderboardNavKey : NavKey

@Serializable
data class UnitedDetailNavKey(val id: String) : NavKey