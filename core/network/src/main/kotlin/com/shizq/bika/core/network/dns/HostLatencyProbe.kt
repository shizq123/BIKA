package com.shizq.bika.core.network.dns

import com.shizq.bika.core.network.dns.HostLatencyProbe.Companion.UNREACHABLE


/**
 * 探测单个 IP 的连通性 / 握手延迟。
 *
 * 具体用 TCP 握手还是别的方式测量，是实现细节。
 */
interface HostLatencyProbe {
    /**
     * 返回到 [ip] 的握手延迟（毫秒）。
     * 不可达或超时返回 [UNREACHABLE]。
     */
    suspend fun measureLatency(ip: String): Long

    companion object {
        /** 不可达 / 超时的哨兵值 */
        const val UNREACHABLE: Long = Long.MAX_VALUE
    }
}
