package com.shizq.bika.core.network.dns

/**
 * 一个通过 DNS 解析服务拿到的直连候选 IP。
 *
 * @param ip 候选 IP 地址
 * @param lineName 分流线路标识（如 telecom / unicom / mobile / overseas）
 * @param domain 该 IP 服务的目标域名（[BikaDnsDomains] 之一）
 */
data class ResolvedDnsHost(
    val ip: String,
    val lineName: String,
    val domain: String,
)

/**
 * 负责从远端 DNS 解析服务拉取比卡（Bika）各域名的直连候选 IP。
 *
 * 具体走 HTTP / 何种解析协议、请求哪些端点，都是实现细节，调用方只关心结果。
 */
interface DnsHostResolver {
    /** 拉取所有受支持域名的候选 IP，失败的域名返回空、不抛异常。 */
    suspend fun resolveHosts(): List<ResolvedDnsHost>
}

/** 比卡直连涉及的域名常量，供解析结果分类与分流决策使用。 */
object BikaDnsDomains {
    /** 图片存储域名 */
    const val IMAGE = "picacomic.com"

    /** API 域名 */
    const val API = "picaapi.picacomic.com"
}
