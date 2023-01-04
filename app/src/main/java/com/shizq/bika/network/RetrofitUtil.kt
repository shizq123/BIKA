package com.shizq.bika.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtil {
    private var retrofit: Retrofit? = null

    var INFO = "http://68.183.234.72/"
    var BASE_URL = "https://picaapi.picacomic.com"
    var UPDATE = "https://raw.githubusercontent.com/"
    var URL = "" //用于记录


    val service: ApiService by lazy {
        getRetrofit(BASE_URL).create(ApiService::class.java)
    }

    // TODO 先实现效果 以后修改
    val service_init: ApiService by lazy {
        getRetrofit(INFO).create(ApiService::class.java)
    }
    val service_update: ApiService by lazy {
        getRetrofit(UPDATE).create(ApiService::class.java)
    }

    //BASE_URL
    private fun getRetrofit(url:String): Retrofit {
        if (retrofit == null||URL!=url) {
            URL=url
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        }
        return retrofit!!
    }

    private fun getOkHttpClient(): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
//            .retryOnConnectionFailure(true)// 错误重连
            .dns(HttpDns())
        return builder.build()
    }
}