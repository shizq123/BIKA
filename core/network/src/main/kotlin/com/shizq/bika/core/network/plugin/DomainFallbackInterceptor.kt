package com.shizq.bika.core.network.plugin

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

private const val TAG = "DomainFallbackCoil"

private class FallbackMarker : AbstractCoroutineContextElement(FallbackMarker) {
    companion object Key : CoroutineContext.Key<FallbackMarker>
}

class DomainFallbackInterceptor : Interceptor {
    @Volatile
    private var optimalFallbackHost: String? = null

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = coroutineScope {
        if (coroutineContext[FallbackMarker] != null) {
            Log.d(TAG, "Skipping fallback race for fallback request: ${chain.request.data}")
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

        val resultChannel = Channel<ImageResult>(1)

        // 1. 启动主域名图片下载请求
        val mainJob = launch {
            try {
                val result = chain.proceed()
                resultChannel.trySend(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                resultChannel.trySend(ErrorResult(null, chain.request, e))
            }
        }

        // 2. 启动慢速检测协程：如果 2.5 秒后主请求没完，或者 2.5 秒内主请求挂了，启动并发竞速
        val fallbackJob = launch {
            try {
                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < 2500L) {
                    if (!mainJob.isActive) {
                        break
                    }
                    delay(100L)
                }

                if (mainJob.isActive) {
                    Log.w(TAG, "Main request for '$failedHost' is too slow (>2.5s). Starting fallback race.")
                } else {
                    Log.w(TAG, "Main request for '$failedHost' failed quickly. Starting fallback race immediately.")
                }

                val raceResult = performFallbackRace(chain, httpUrl, failedHost)
                if (raceResult != null) {
                    resultChannel.trySend(raceResult)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                resultChannel.close()
            }
        }

        var finalResult: ImageResult? = null

        for (result in resultChannel) {
            if (result is SuccessResult) {
                finalResult = result
                break
            } else if (result is ErrorResult) {
                if (!mainJob.isActive && !fallbackJob.isActive) {
                    finalResult = result
                    break
                }
            }
        }

        mainJob.cancel()
        fallbackJob.cancel()

        finalResult ?: chain.proceed()
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
            Log.d(TAG, "Trying fast path with known optimal host: $currentOptimal")
            val fastResult = tryFallbackHost(chain, httpUrl, currentOptimal)
            if (fastResult != null) {
                Log.i(TAG, "Fast path successful with: $currentOptimal")
                return fastResult
            }
            Log.d(TAG, "Fast path failed. Falling back to race.")
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
        Log.i(TAG, "Racing remaining hosts: $hostsToRace")

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
            Log.i(TAG, "Race won by host: '${msg.first}'")
            break
        }

        jobs.forEach { it.cancel() }

        if (winnerResult == null) {
            Log.e(TAG, "All fallback attempts failed for: $originalHttpUrl")
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