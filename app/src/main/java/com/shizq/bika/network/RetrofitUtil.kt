package com.shizq.bika.network

object RetrofitUtil {
    private const val BASE_URL = "https://picaapi.picacomic.com"
    var LIVE_SERVER = "https://live-server.bidobido.xyz"//新聊天室
    private var URL: String? = null //用于记录
    val service: ApiService by lazy {
        TODO()
    }
    val service_live: ApiService by lazy {
        TODO()
    }
}