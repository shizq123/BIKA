package com.shizq.bika.core.model.preferences

import com.shizq.bika.core.model.theme.DarkThemeConfig
import kotlinx.serialization.Serializable

@Serializable
data class ThemePreferences(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
)