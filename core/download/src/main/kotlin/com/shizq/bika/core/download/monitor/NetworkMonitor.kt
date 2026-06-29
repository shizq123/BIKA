package com.shizq.bika.core.download.monitor

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    fun isConnected(): Boolean
    fun isWifiConnected(): Boolean

    /**
     * 以流的形式发出当前是否有可用网络。
     * 每当连接状态发生变化时发出最新值，用于在网络恢复时触发等待任务的重新调度。
     */
    val isOnline: Flow<Boolean>
}