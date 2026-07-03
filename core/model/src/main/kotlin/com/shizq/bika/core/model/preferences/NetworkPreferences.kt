package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class NetworkPreferences(
    val dns: DnsPreferences = DnsPreferences(),
    val isLoggingEnabled: Boolean = false,
)