package com.shizq.bika.core.network.auth

import com.shizq.bika.core.network.plugin.UnauthorizedException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey

private val logger = KotlinLogging.logger("SessionExpiry")

/**
 * 标记不参与会话终止判定的请求。
 *
 * 登录接口自身的 401 表示"这次输入的账号密码不对"，属于登录表单的业务错误，
 * 不能被解读为"已有会话过期"——否则会在用户尚未登录时触发一次无意义的
 * 会话终止。引导配置等匿名接口同理。
 */
val SkipSessionExpiry = AttributeKey<Unit>("SkipSessionExpiry")

/**
 * 拦截 HTTP 401 并终止会话。
 *
 * Bika 的 401 有两条通路，本插件负责传输层那条：
 * - **通路 A（本插件）**：HTTP 状态码 401。
 * - **通路 B**：HTTP 200 + 响应体 `code=401`，由
 *   [com.shizq.bika.core.network.plugin.ApiEnvelopePlugin] 的 `onUnauthorized`
 *   回调处理。
 *
 * 两条通路都汇聚到 [SessionManager.terminateSession]，对外抛出同一个
 * [UnauthorizedException]，上层无需区分。
 *
 * 顺带修掉一个旧缺陷：此前 HTTP 401 会因 `!status.isSuccess()` 被信封插件
 * 原样透传，随后 `body<T>()` 抛出 `NoTransformationFoundException`，
 * 上层拿到的是难以理解的序列化错误而非鉴权错误。
 */
fun sessionExpiryPlugin(sessionManager: SessionManager): ClientPlugin<Unit> =
    createClientPlugin("SessionExpiry") {
        on(Send) { request ->
            val call = proceed(request)
            if (call.response.status == HttpStatusCode.Unauthorized &&
                request.attributes.getOrNull(SkipSessionExpiry) == null
            ) {
                logger.warn { "收到 HTTP 401：${request.method.value} ${request.url.encodedPath}" }
                sessionManager.terminateSession(SessionExpiryReason.TokenRejected)
                throw UnauthorizedException("登录已过期，请重新登录")
            }
            call
        }
    }
