package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object DashboardNavKey : NavKey

@Serializable
sealed interface FeedNavKey : NavKey {
    @Serializable
    object Collection : FeedNavKey

    @Serializable
    object Random : FeedNavKey

    @Serializable
    data class Topic(val name: String) : FeedNavKey

    @Serializable
    object Recent : FeedNavKey

    @Serializable
    data class Knight(val name: String, val id: String) : FeedNavKey
}

@Serializable
object LeaderboardNavKey : NavKey

@Serializable
data class UnitedDetailNavKey(val id: String) : NavKey

@Serializable
data class ReaderNavKey(val id: String, val order: Int) : NavKey