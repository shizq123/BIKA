package com.shizq.bika.core.network.plugin

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val TAG = "DomainFallbackCoil"

class DomainFallbackInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val initialResult = chain.proceed()

        if (initialResult !is ErrorResult || chain.request.data !is String) {
            return initialResult
        }

        val originalUrl = chain.request.data as String
        val httpUrl = originalUrl.toHttpUrlOrNull() ?: return initialResult

        val failedHost = httpUrl.host
        val allManagedHosts = DomainConfig.imageDomains.mapNotNull { it.toHttpUrlOrNull()?.host }

        if (failedHost !in allManagedHosts) {
            return initialResult
        }

        Log.w(
            TAG,
            "Initial request to host '$failedHost' failed. Starting fallback mechanism.",
            initialResult.throwable
        )

        val fallbackHosts = allManagedHosts.filter { it != failedHost }
        if (fallbackHosts.isEmpty()) {
            Log.w(TAG, "No fallback domains available for '$failedHost'.")
            return initialResult
        }

        return raceFallbackRequests(chain, httpUrl, fallbackHosts) ?: initialResult
    }

    private suspend fun raceFallbackRequests(
        chain: Interceptor.Chain,
        originalHttpUrl: okhttp3.HttpUrl,
        fallbackHosts: List<String>
    ): SuccessResult? = coroutineScope {
        Log.i(TAG, "Racing fallback requests for hosts: $fallbackHosts")

        val deferredResults = fallbackHosts.map { host ->
            async {
                val newUrl = originalHttpUrl.newBuilder().host(host).build().toString()
                val newRequest = chain.request.newBuilder().data(newUrl).build()
                Log.d(TAG, "Starting fallback attempt for '$host' with url: $newUrl")
                when (val result = chain.withRequest(newRequest).proceed()) {
                    is SuccessResult -> {
                        Log.i(TAG, "Fallback successful with host: '$host'")
                        result
                    }

                    is ErrorResult -> {
                        Log.w(
                            TAG,
                            "Fallback attempt for host '$host' failed.",
                            result.throwable
                        )
                        null
                    }
                }
            }
        }

        val firstSuccess = deferredResults
            .asFlow()
            .map { it.await() }
            .filterIsInstance<SuccessResult>()
            .firstOrNull()

        if (firstSuccess == null) {
            Log.e(TAG, "All fallback attempts failed for original url: $originalHttpUrl")
        }

        firstSuccess
    }
}

object DomainConfig {
    val imageDomains = listOf(
        "https://s3.picacomic.com",
        "https://s2.picacomic.com",
        "https://storage1.picacomic.com",
        "https://storage-b.picacomic.com",
    )
}