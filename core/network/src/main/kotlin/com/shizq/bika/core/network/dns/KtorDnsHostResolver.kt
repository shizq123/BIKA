package com.shizq.bika.core.network.dns

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

/**
 * 基于 HTTP DNS 解析端点的 [DnsHostResolver] 实现，走项目统一的 Ktor 网络栈。
 *
 * 注入 [Named] 为 "dns" 的独立 [HttpClient]（见 NetworkModule）：不带鉴权、
 * 不走 DirectDns，避免"解析直连 IP 却依赖直连 IP"的循环依赖。
 *
 * 远端响应的外层信封 `{code,message,data}` 由 ApiEnvelopePlugin 统一解包，
 * 因此 [DnsResolveData] 直接映射 `data` 层。
 */
@Singleton
internal class KtorDnsHostResolver @Inject constructor(
    @Named("dns") private val client: HttpClient,
) : DnsHostResolver {
    private val requests = listOf(
        "https://macapi1.com/app/picacomic/dns/resolve?domain=${BikaDnsDomains.IMAGE}" to BikaDnsDomains.IMAGE,
        "https://macapi2.com/app/picacomic/dns/resolve?domain=${BikaDnsDomains.API}" to BikaDnsDomains.API,
    )

    override suspend fun resolveHosts(): List<ResolvedDnsHost> = coroutineScope {
        requests
            .map { (url, domain) -> async { fetchIpsForDomain(url, domain) } }
            .flatMap { it.await() }
            .distinctBy { it.ip }
    }

    /**
     * 拉取单个域名的候选 IP。把 `lines` 里每条线路的 `ips` 摊平成
     * 带线路标识的 [ResolvedDnsHost]。任何失败都降级为空列表，不抛出。
     */
    private suspend fun fetchIpsForDomain(
        url: String,
        domain: String,
    ): List<ResolvedDnsHost> = try {
        client.get(url).body<DnsResolveData>()
            .lines
            .orEmpty()
            .flatMap { (lineName, line) ->
                line.ips.map { ip ->
                    ResolvedDnsHost(ip = ip, lineName = lineName, domain = domain)
                }
            }
    } catch (_: Exception) {
        emptyList()
    }
}

@Serializable
private data class DnsResolveData(
    val domain: String,
    val lines: Map<String, DnsLine>? = null,
)

@Serializable
private data class DnsLine(
    val ips: List<String> = emptyList(),
    val status: String = "",
)
