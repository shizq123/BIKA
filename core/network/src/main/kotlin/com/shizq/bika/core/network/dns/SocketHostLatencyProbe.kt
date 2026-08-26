package com.shizq.bika.core.network.dns

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

/** 通过对 443 端口做一次 TCP 握手来测量延迟的 [HostLatencyProbe] 实现。 */
@Singleton
internal class SocketHostLatencyProbe @Inject constructor() : HostLatencyProbe {

    override suspend fun measureLatency(ip: String): Long = withContext(Dispatchers.IO) {
        measureTimeMillis {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, HTTPS_PORT), CONNECT_TIMEOUT_MS)
                }
            } catch (e: Exception) {
                HostLatencyProbe.UNREACHABLE
            }
        }

    }

    private companion object {
        const val HTTPS_PORT = 443
        const val CONNECT_TIMEOUT_MS = 2000
    }
}
