package com.shizq.bika.core.network.plugin

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

private const val TAG = "DomainFallbackCoil"

class DomainFallbackInterceptor : Interceptor {
    @Volatile
    private var optimalFallbackHost: String? = null

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val initialResult = chain.proceed()

        if (initialResult !is ErrorResult || chain.request.data !is String) {
            return initialResult
        }

        val originalUrl = chain.request.data as String
        val httpUrl = originalUrl.toHttpUrl()
        val failedHost = httpUrl.host

        if (failedHost !in DomainConfig.MANAGED_HOSTS) {
            return initialResult
        }

        Log.w(TAG, "Host '$failedHost' failed. Starting fallback.", initialResult.throwable)

        val fallbackHosts = DomainConfig.MANAGED_HOSTS.filter { it != failedHost }
        if (fallbackHosts.isEmpty()) return initialResult

        // ==========================================
        // 策略 1: Fast Path (快速通道)
        // 如果有已知的最佳域名，先单独尝试它，避免并发风暴
        // ==========================================
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
        if (hostsToRace.isEmpty()) return initialResult

        // ==========================================
        // 策略 2: Race Path (剩余域名通道并发竞速)
        // ==========================================
        return raceWithChannel(chain, httpUrl, hostsToRace) ?: initialResult
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
                    // 尝试发送，如果 Channel 已满（说明别人先成功了）则忽略
                    resultChannel.trySend(host to result)
                }
            }
        }

        // 启动一个独立的协程：当所有任务结束后，关闭 Channel 防止死锁等待
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
            withTimeoutOrNull(1000L) {
                val result = chain.withRequest(newRequest).proceed()
                result as? SuccessResult
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