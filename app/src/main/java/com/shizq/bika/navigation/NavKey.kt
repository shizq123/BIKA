package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object DashboardNavKey : NavKey

@Serializable
sealed interface FeedNavKey : NavKey {
    object Collection : FeedNavKey
    object Random : FeedNavKey
}