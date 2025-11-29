package com.shizq.bika.core.network.plugin

import coil3.intercept.Interceptor
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageResult

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val originalRequest = chain.request
        val token = tokenProvider()

        if (token.isNullOrEmpty()) {
            return chain.proceed()
        }

        val newRequest = originalRequest.newBuilder()
            .apply {
                val networkHeaders = NetworkHeaders.Builder()
                    .set("Authorization", token)
                    .build()
                httpHeaders(networkHeaders)
            }
            .build()

        return chain.withRequest(newRequest).proceed()
    }
}