package com.shizq.bika.network

import retrofit2.Retrofit

object RetrofitUtil {
    private var retrofit: Retrofit? = null
    private const val BASE_URL = "https://picaapi.picacomic.com"
    var LIVE_SERVER = "https://live-server.bidobido.xyz"//新聊天室
    private var URL: String? = null //用于记录
    val service: ApiService by lazy {
        getRetrofit(BASE_URL).create(ApiService::class.java)
    }

    private fun getRetrofit(url: String): Retrofit {
        if (retrofit == null || URL != url) {
            URL = url//记录baseurl
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .build()
        }
        return retrofit!!
    }
}