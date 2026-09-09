package com.shizq.bika.core.network

/**
 * Bika API 的域名常量，单一事实来源。
 *
 * 此前该域名字符串在 [com.shizq.bika.core.network.di.NetworkModule]、
 * [com.shizq.bika.core.network.plugin.BikaSignatureAuth]、
 * [com.shizq.bika.core.network.plugin.DirectDns] 三处各写一份，
 * 改一次域名要记得同步三处；漏改的后果是签名计算用错误的 urlPath 算出
 * 错误 signature，只会表现为"某些请求莫名其妙 403/签名校验失败"，且不会在
 * 编译期或明显的运行期报错中体现。
 */
object BikaEndpoints {
    const val API_HOST: String = "picaapi.picacomic.com"
    const val API_BASE_URL: String = "https://$API_HOST"
}
