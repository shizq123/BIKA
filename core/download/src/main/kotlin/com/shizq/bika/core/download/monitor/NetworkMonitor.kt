package com.shizq.bika.core.download.monitor

interface NetworkMonitor {
    fun isConnected(): Boolean
    fun isWifiConnected(): Boolean
}