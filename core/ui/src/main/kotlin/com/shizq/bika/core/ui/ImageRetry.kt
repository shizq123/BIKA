package com.shizq.bika.core.ui

import coil3.compose.AsyncImagePainter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val logger = KotlinLogging.logger("ImageRetry")

/**
 * 判断加载失败是否值得自动重试：
 * - HTTP 4xx（404 资源不存在/403 无权限等）是永久性失败，重试无意义，不自动重试；
 * - 其余（网络抖动、超时、DNS 失败、5xx 服务器错误）属于临时性失败，持续退避重试。
 */
fun Throwable?.isRetryableError(): Boolean =
    this !is coil3.network.HttpException || response.code >= 500

/** 退避曲线的封顶间隔。 */
private const val MaxBackoffMillis = 30_000L

/** 达到 [MaxBackoffMillis] 所需的位移次数：2000 shl 4 = 32000 > 30000。 */
private const val MaxBackoffShift = 4

/**
 * 第 [attempt] 次重试前应等待的毫秒数：2s / 4s / 8s / 16s / 30s 封顶。
 *
 * 位移量必须先夹进 `0..MaxBackoffShift` 再移，不能移完再 `coerceAtMost`：
 * `shl` 的右操作数按 mod 32 取模，`attempt` 涨到 32 时 `2000L shl 32` 会绕回
 * 2000，退避曲线直接失效、退化成 2s 一次的无限轮询。负数同理（mod 后是大数）。
 *
 * 抽成纯函数是因为这条曲线原先在三处各写了一遍并且已经漂移：两处做了钳制、
 * `RetryableAsyncImage` 没做。
 */
fun backoffDelayMillis(attempt: Int): Long =
    (2000L shl attempt.coerceIn(0, MaxBackoffShift)).coerceAtMost(MaxBackoffMillis)

/**
 * 图片加载失败后的退避重试循环。挂起直到被取消，因此调用方应放在
 * `LaunchedEffect(painter, retryNonce)` 里，靠取消/重启来管理生命周期。
 *
 * ## 为什么是一条长活协程，而不是「计数 + LaunchedEffect(计数)」
 *
 * 重试计数是**协程栈上的局部变量**。三处调用点原先都是「用 `remember` 存计数，
 * 再把计数当成 `LaunchedEffect` 的 key」，也就是 effect 自己写自己的 key：
 * 自增 → 重组 → 协程在 `delay` 上被取消 → `restart()` 永远走不到 → 新协程启动
 * 后立刻又自增。净效果是自动重试一次都不发生，外加一个无界重组循环
 * （只要屏上有一张图停在可重试错误态就持续烧 CPU 和电）。
 *
 * 计数活在栈上就不可能成为 key，「先自增还是先 delay」也不再是正确性问题。
 * 同一条协程贯穿整个错误态序列，计数也不会因为 state 短暂离开 Error
 * （重试后进入 Loading）而丢失，退避不会被拉回 2s。
 *
 * ## 为什么循环是自驱动的
 *
 * 每轮都用 `first { }` 主动去要一个错误态，而不是靠 [AsyncImagePainter.state]
 * 再发射一次新的 Error 来推进。它是 `StateFlow`，会按相等性去重——把「下一轮
 * 重试」挂在「两次失败发射的值不相等」这个假设上，一旦相等重试就永久停摆。
 * 当前值已经是 Error 时 `first { }` 立即返回，因此推进不依赖发射；而每轮必经
 * 一次 [delay]，因此也不会退化成忙循环。
 *
 * 计数只随本协程的生命周期重置（模型变化、节点重建、调用方递增 nonce），
 * 加载成功后不重置：长期存活的节点如果反复恢复又失败，说明它确实不稳定，
 * 沿用较长的间隔是想要的行为。
 *
 * @param describe 仅在需要写日志时求值，用于定位是哪张图（页码 / url / model）。
 */
suspend fun AsyncImagePainter.autoRetryOnError(describe: () -> String) {
    var attempt = 0
    var logged = false
    while (true) {
        val error = state
            .first { it is AsyncImagePainter.State.Error }
            .let { (it as AsyncImagePainter.State.Error).result.throwable }

        // 只记首次失败，避免按可见项数量 × 重试轮数刷屏。
        if (!logged) {
            logged = true
            if (error.isRetryableError()) {
                logger.error(error) { "图片加载失败: ${describe()}" }
            } else {
                logger.warn(error) { "图片永久不可用(不重试): ${describe()}" }
            }
        }
        // 永久失败：结束协程。后续只可能由用户显式重试（调用方递增 nonce）重启。
        if (!error.isRetryableError()) return

        delay(backoffDelayMillis(attempt))
        attempt++
        // 等待期间可能已被手动重试救回来了，别再补一次多余的请求。
        if (state.value is AsyncImagePainter.State.Error) {
            restart()
        }
    }
}
