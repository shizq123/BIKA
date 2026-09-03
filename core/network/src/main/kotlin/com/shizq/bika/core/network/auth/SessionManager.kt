package com.shizq.bika.core.network.auth

import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageId
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportWarning
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger("Session")

/** 会话终止原因，用于向用户呈现不同文案。 */
enum class SessionExpiryReason {
    /** 服务端拒绝了当前 token（HTTP 401 或响应体 code=401）。 */
    TokenRejected,
}

/**
 * 会话生命周期的唯一权威入口。
 *
 * Bika 服务端不提供 refresh token，唯一的续期手段是用账号密码重新登录。
 * 本项目不采用后台静默重登（避免在设备上长期驻留可用密码，也避免并发 401
 * 引发的重登风暴与 OkHttp 连接池死锁），因此 401 的语义被收敛为：
 * **会话终止，交还给用户显式登录**。
 *
 * 终止动作只清 token，不清用户名/密码：
 * - 清 token 会让 [UserCredentialsDataSource.userData] 的 token 变空，
 *   MainActivityViewModel 据此把 startDestination 切到登录页，
 *   路由跳转因此是数据驱动的，不需要额外的导航事件。
 * - 保留用户名/密码使登录页仍能预填，减少重新登录的输入成本。
 *   显式登出应另行清除全部凭据。
 */
@Singleton
class SessionManager @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val messageReporter: MessageReporter,
) {
    /**
     * 幂等闸门：token 失效时往往有多个并发请求同时收到 401，
     * 这里保证只清一次 token、只提示一次。
     */
    private val terminated = AtomicBoolean(false)

    /**
     * 终止当前会话。并发调用只有首个生效。
     *
     * 清 token 用 [NonCancellable] 包裹：401 通常发生在被取消的页面请求上，
     * 若写入随请求一起被取消，会留下「token 已被服务端拒绝但本地仍存在」的
     * 僵死状态，用户既进不去也不会被送回登录页。
     */
    suspend fun terminateSession(reason: SessionExpiryReason) {
        if (!terminated.compareAndSet(false, true)) {
            logger.debug { "会话已在终止流程中，忽略重复的 401（reason=$reason）" }
            return
        }
        logger.info { "会话终止：$reason，清除本地 token" }
        withContext(NonCancellable) {
            userCredentialsDataSource.setToken(null)
        }
        messageReporter.reportWarning(
            text = UiText.of("登录已过期，请重新登录"),
            duration = MessageDuration.Long,
            // 固定 id：即使闸门之外仍有并发上报，消息层也会按 id 合并。
            id = SessionExpiredMessageId,
        )
    }

    /**
     * 登录成功后调用，重置幂等闸门，使下一次 token 失效仍能被正常处理。
     */
    fun onAuthenticated() {
        terminated.set(false)
        logger.debug { "会话已建立，重置会话终止闸门" }
    }

    private companion object {
        val SessionExpiredMessageId = MessageId("session-expired")
    }
}
