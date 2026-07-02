package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class DnsPreferences(
    val apiDns: Set<String> = setOf(DEFAULT_DNS_IP),
    val imageDns: Set<String> = setOf(DEFAULT_DNS_IP),
    val activeLine: String = DEFAULT_DNS_LINE,
) {
    companion object {
        const val DEFAULT_DNS_IP = "104.21.20.188"
        const val DEFAULT_DNS_LINE = "telecom"
    }
}
