package com.shizq.bika.network

import java.io.IOException
import java.lang.reflect.Proxy

object RetrofitUtil {
    private const val BASE_URL = "https://picaapi.picacomic.com"
    var LIVE_SERVER = "https://live-server.bidobido.xyz"//新聊天室
    private var URL: String? = null //用于记录

    private fun createFallbackService(): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, _, _ ->
            throw IOException("Network API is not implemented yet in new architecture")
        } as ApiService
    }

    val service: ApiService by lazy {
        createFallbackService()
    }
    val service_live: ApiService by lazy {
        createFallbackService()
    }
}