package com.shizq.bika.core.network.dns

/**
 * 服务端 `app-channel` 请求头的取值：按运营商线路分流。
 *
 * "1"/"2"/"3" 是服务端约定的业务协议值，不是随意的内部编号，因此单独用
 * 命名函数表达，而不是直接埋在 DI 的 lambda 里。[lineName] 来自用户偏好里的
 * `network.dns.activeLine`（"telecom" / "unicom" / "mobile" 等字符串约定）。
 *
 * 未识别的线路默认电信（与服务端默认行为一致），而非"配置异常"，因此这里
 * 不抛异常、不记日志，直接回退。
 */
internal fun appChannelHeaderFor(lineName: String): String = when (lineName.lowercase()) {
    "telecom" -> "1"
    "unicom" -> "2"
    "mobile" -> "3"
    else -> "1" // 未知线路回退电信
}
