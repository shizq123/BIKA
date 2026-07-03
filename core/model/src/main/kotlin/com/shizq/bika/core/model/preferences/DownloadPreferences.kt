package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class DownloadPreferences(
    val overWifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
)