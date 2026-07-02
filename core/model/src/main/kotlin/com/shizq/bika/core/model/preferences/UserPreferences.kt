package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val reader: ReaderPreferences = ReaderPreferences(),
    val theme: ThemePreferences = ThemePreferences(),
    val network: NetworkPreferences = NetworkPreferences(),
    val download: DownloadPreferences = DownloadPreferences(),
    val filter: ContentFilterPreferences = ContentFilterPreferences(),
    val app: AppPreferences = AppPreferences(),
    val dashboard: DashboardPreferences = DashboardPreferences(),
    val profile: UserProfileSnapshot = UserProfileSnapshot(),
)