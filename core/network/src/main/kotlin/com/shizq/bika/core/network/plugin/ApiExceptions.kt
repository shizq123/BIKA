package com.shizq.bika.core.network.plugin

/** [serverMessage] 是服务端信封里的原始 message，供调用方在 UI 上直接展示。 */
class ApiException(val code: Int, val serverMessage: String) :
    Exception("API Error ($code): $serverMessage")

/**
 * 鉴权失败的统一异常，由两条通路共同抛出：
 * - HTTP 401（见 [com.shizq.bika.core.network.auth.sessionExpiryPlugin]）
 * - HTTP 200 + 信封内 `code=401`（见 [ApiEnvelopePlugin]）
 */
class UnauthorizedException(message: String) : Exception(message)