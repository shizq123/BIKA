package com.shizq.bika.core.ui

import coil3.compose.AsyncImagePainter
import coil3.network.HttpException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger("ImageRetry")

private const val MaxAutoRetries = 4
private const val MaxRetryDurationMillis = 30_000L
private const val MaxBackoffMillis = 30_000L
private const val MaxBackoffShift = 4

/** 只允许明确判定为临时性故障的错误进入自动重试。 */
enum class ImageRetryDecision {
    Retry,
    Stop,
}

fun Throwable?.retryDecision(): ImageRetryDecision = when (this) {
    is HttpException -> {
        if (response.code in 400..499) {
            ImageRetryDecision.Stop
        } else {
            ImageRetryDecision.Retry
        }
    }

    null -> ImageRetryDecision.Stop
    is IOException -> ImageRetryDecision.Retry
    else -> ImageRetryDecision.Stop
}

fun backoffDelayMillis(attempt: Int): Long =
    (2000L shl attempt.coerceIn(0, MaxBackoffShift))
        .coerceAtMost(MaxBackoffMillis)

sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Retrying(val attempt: Int) : ImageLoadState
    data object Success : ImageLoadState
    data class Failed(val decision: ImageRetryDecision) : ImageLoadState
}

/**
 * 负责单个 Painter 的完整加载生命周期。
 *
 * Painter 只负责执行和绘制请求；重试上限、请求代数、错误分类和 UI 状态由此控制器管理。
 * 调用方必须为每次 model 变化或手动重试重新调用 [run]，且不得直接调用 painter.restart()。
 */
class ImageRetryController(
    private val painter: AsyncImagePainter,
) {
    private val _state = MutableStateFlow<ImageLoadState>(ImageLoadState.Loading)
    val state: StateFlow<ImageLoadState> = _state.asStateFlow()

    private var generation = 0L

    suspend fun run(modelKey: String) {
        val currentGeneration = ++generation
        val startedAt = System.nanoTime()
        var attempt = 0
        var logged = false

        _state.value = ImageLoadState.Loading
        painter.restart()

        try {
            while (currentGeneration == generation) {
                // 必须等本轮 restart 发出 Loading，不能消费上一轮残留的 Success/Error。
                painter.state.first {
                    it is AsyncImagePainter.State.Loading
                }
                val terminalState = painter.state.first {
                    it is AsyncImagePainter.State.Error ||
                            it is AsyncImagePainter.State.Success
                }


                if (terminalState is AsyncImagePainter.State.Success) {
                    _state.value = ImageLoadState.Success
                    return
                }

                val error = (terminalState as AsyncImagePainter.State.Error)
                    .result.throwable
                val decision = error.retryDecision()

                if (!logged) {
                    logged = true
                    val safeKey = safeImageKey(modelKey)
                    if (decision == ImageRetryDecision.Retry) {
                        logger.error(error) {
                            "图片加载失败: key=$safeKey"
                        }
                    } else {
                        logger.warn(error) {
                            "图片不可重试: key=$safeKey"
                        }
                    }
                }

                val durationExceeded =
                    (System.nanoTime() - startedAt) / 1_000_000L >= MaxRetryDurationMillis
                if (decision == ImageRetryDecision.Stop ||
                    attempt >= MaxAutoRetries ||
                    durationExceeded
                ) {
                    _state.value = ImageLoadState.Failed(decision)
                    return
                }

                val nextAttempt = attempt + 1
                _state.value = ImageLoadState.Retrying(nextAttempt)
                delay(backoffDelayMillis(attempt).milliseconds)

                if (currentGeneration != generation) return
                attempt = nextAttempt
                painter.restart()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    fun invalidate() {
        generation++
    }
}

/** 日志中只保留不可逆的短标识，不记录原始 URL、token 或请求对象。 */
private fun safeImageKey(value: String): String =
    value.substringBefore('?').substringBefore('#').hashCode().toUInt().toString(16)
