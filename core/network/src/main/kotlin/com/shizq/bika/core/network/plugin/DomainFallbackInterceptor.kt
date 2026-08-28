package com.shizq.bika.core.network.plugin

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.shizq.bika.core.network.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private const val TAG = "DomainFallbackCoil"
private val DEBUG_LOGGING = BuildConfig.DEBUG

/** 主请求慢于此值就启动降级竞速。 */
private const val SLOW_MAIN_THRESHOLD_MS = 2500L

/**
 * 4xx 是永久性失败：资源在这个 path 上就是不存在（404）或无权限（403），
 * 换域名重试同一个 path 不会有不同结果，只会放大成 N 倍无效请求。
 * 只有 5xx 与传输层异常（超时、DNS、连接失败）才值得换域名。
 */
private fun Throwable?.isWorthFallback(): Boolean =
    this !is coil3.network.HttpException || response.code >= 500

private class FallbackMarker : AbstractCoroutineContextElement(FallbackMarker) {
    companion object Key : CoroutineContext.Key<FallbackMarker>
}

class DomainFallbackInterceptor : Interceptor {
    @Volatile
    private var optimalFallbackHost: String? = null

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = coroutineScope {
        if (coroutineContext[FallbackMarker] != null) {
            if (DEBUG_LOGGING) Log.d(TAG, "Skipping fallback race for fallback request: ${chain.request.data}")
            return@coroutineScope chain.proceed()
        }

        if (chain.request.data !is String) {
            return@coroutineScope chain.proceed()
        }

        val originalUrl = chain.request.data as String
        val httpUrl = try {
            originalUrl.toHttpUrl()
        } catch (e: Exception) {
            return@coroutineScope chain.proceed()
        }
        val failedHost = httpUrl.host

        if (failedHost !in DomainConfig.MANAGED_HOSTS) {
            return@coroutineScope chain.proceed()
        }

        // 主域名请求。异常在内部收成 ErrorResult，避免抛出去连带取消整个 coroutineScope。
        val mainRequest = async {
            try {
                chain.proceed()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ErrorResult(null, chain.request, e)
            }
        }

        // 等一小会儿：主请求可能很快成功，也可能很快失败。
        val earlyResult = withTimeoutOrNull(SLOW_MAIN_THRESHOLD_MS) { mainRequest.await() }

        if (earlyResult is SuccessResult) {
            return@coroutineScope earlyResult
        }

        // 主请求已经失败：只有值得降级的错误才换域名。404/403 直接返回，
        // 否则一个必然失败的 path 会被放大成 7 个请求（fast path + 5 竞速 + 收尾 proceed）。
        if (earlyResult is ErrorResult) {
            if (!earlyResult.throwable.isWorthFallback()) {
                if (DEBUG_LOGGING) {
                    Log.w(TAG, "Permanent failure, skipping fallback: ${chain.request.data}", earlyResult.throwable)
                }
                return@coroutineScope earlyResult
            }
            if (DEBUG_LOGGING) Log.w(TAG, "Main request for '$failedHost' failed. Starting fallback race.")
        } else {
            if (DEBUG_LOGGING) {
                Log.w(TAG, "Main request for '$failedHost' is too slow (>${SLOW_MAIN_THRESHOLD_MS}ms). Starting fallback race.")
            }
        }

        val raceResult = performFallbackRace(chain, httpUrl, failedHost)
        if (raceResult != null) {
            mainRequest.cancel()
            return@coroutineScope raceResult
        }

        // 竞速全败。主请求可能还在跑（慢速分支），等它的真实结果，
        // 而不是再发一次 chain.proceed()。
        val mainResult = try {
            mainRequest.await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ErrorResult(null, chain.request, e)
        }
        if (DEBUG_LOGGING && mainResult is ErrorResult) {
            Log.e(TAG, "All attempts failed for: $originalUrl")
        }
        mainResult
    }

    /**
     * 并发竞速降级策略核心，集成了已知最优域名快速通道（Fast Path）与并发竞速通道
     */
    private suspend fun performFallbackRace(
        chain: Interceptor.Chain,
        httpUrl: HttpUrl,
        failedHost: String
    ): ImageResult? {
        val fallbackHosts = DomainConfig.MANAGED_HOSTS.filter { it != failedHost }
        if (fallbackHosts.isEmpty()) return null

        // 策略 1: Fast Path (快速通道)
        // 如果有已知的最佳域名，先单独尝试它，避免并发风暴
        val currentOptimal = optimalFallbackHost
        if (currentOptimal != null && currentOptimal in fallbackHosts) {
            if (DEBUG_LOGGING) Log.d(TAG, "Trying fast path with known optimal host: $currentOptimal")
            val fastResult = tryFallbackHost(chain, httpUrl, currentOptimal)
            if (fastResult != null) {
                if (DEBUG_LOGGING) Log.i(TAG, "Fast path successful with: $currentOptimal")
                return fastResult
            }
            // 失效即清空：否则这个域名一旦挂掉，后续每张图都要先白等它 3 秒超时。
            if (optimalFallbackHost == currentOptimal) {
                optimalFallbackHost = null
            }
            if (DEBUG_LOGGING) Log.d(TAG, "Fast path failed, cleared optimal host. Falling back to race.")
        }

        // 去除刚才已经试过的最佳域名，剩下的一起竞速
        val hostsToRace = fallbackHosts.filter { it != currentOptimal }
        if (hostsToRace.isEmpty()) return null

        // 策略 2: Race Path (剩余域名通道并发竞速)
        return raceWithChannel(chain, httpUrl, hostsToRace)
    }

    /**
     * 使用 Channel 实现优雅且高性能的竞速
     */
    private suspend fun raceWithChannel(
        chain: Interceptor.Chain,
        originalHttpUrl: HttpUrl,
        hostsToRace: List<String>
    ): SuccessResult? = coroutineScope {
        if (DEBUG_LOGGING) Log.i(TAG, "Racing remaining hosts: $hostsToRace")

        val resultChannel = Channel<Pair<String, SuccessResult>>(1)

        val jobs = hostsToRace.map { host ->
            launch {
                val result = tryFallbackHost(chain, originalHttpUrl, host)
                if (result != null) {
                    resultChannel.trySend(host to result)
                }
            }
        }

        launch {
            jobs.joinAll()
            resultChannel.close()
        }

        var winnerResult: SuccessResult? = null

        for (msg in resultChannel) {
            optimalFallbackHost = msg.first
            winnerResult = msg.second
            if (DEBUG_LOGGING) Log.i(TAG, "Race won by host: '${msg.first}'")
            break
        }

        jobs.forEach { it.cancel() }

        if (winnerResult == null) {
            if (DEBUG_LOGGING) Log.e(TAG, "All fallback attempts failed for: $originalHttpUrl")
        }

        winnerResult
    }

    /**
     * 单个备用域名的请求封装（包含独立的超时控制）
     */
    private suspend fun tryFallbackHost(
        chain: Interceptor.Chain,
        originalUrl: HttpUrl,
        newHost: String
    ): SuccessResult? {
        val newUrl = originalUrl.newBuilder().host(newHost).build().toString()
        val newRequest = chain.request.newBuilder().data(newUrl).build()

        return try {
            withTimeoutOrNull(3000L) {
                withContext(FallbackMarker()) {
                    val result = chain.withRequest(newRequest).proceed()
                    result as? SuccessResult
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }
}

private object DomainConfig {
    val imageDomains = listOf(
        "https://s3.picacomic.com",
        "https://s2.picacomic.com",
        "https://storage.diwodiwo.xyz",
        "https://storage1.picacomic.com",
        "https://storage.tipatipa.xyz",
        "https://www.picacomic.com",
        "https://storage-b.picacomic.com",
    )

    val MANAGED_HOSTS: Set<String> by lazy(LazyThreadSafetyMode.NONE) {
        imageDomains.map { it.toHttpUrl().host }.toSet()
    }
}