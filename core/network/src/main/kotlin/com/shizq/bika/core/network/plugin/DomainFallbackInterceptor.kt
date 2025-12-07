package com.shizq.bika.core.network.plugin

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
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

        var lastResult: ImageResult = initialResult

        fallbackHosts.forEachIndexed { index, host ->
            val newUrl = httpUrl.newBuilder().host(host).build().toString()
            val newRequest = chain.request.newBuilder()
                .data(newUrl)
                .build()

            Log.i(TAG, "Fallback attempt #${index + 1}: Trying new domain '$host'")

            val newChain = chain.withRequest(newRequest)
            val fallbackResult = newChain.proceed()

            if (fallbackResult is SuccessResult) {
                Log.i(TAG, "Fallback successful with domain '$host'.")
                return fallbackResult
            } else if (fallbackResult is ErrorResult) {
                Log.w(
                    TAG,
                    "Fallback attempt with domain '$host' failed.",
                    fallbackResult.throwable
                )
            }

            lastResult = fallbackResult
        }

        Log.e(
            TAG,
            "All fallback attempts failed for original host '$failedHost'. Returning last error."
        )
        return lastResult
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