package com.shizq.bika.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtil {
    private var retrofit: Retrofit? = null

    private const val INFO = "http://68.183.234.72"
    private const val BASE_URL = "https://picaapi.picacomic.com"
    private const val UPDATE = "https://appcenter.ms"
    var LIVE_SERVER = "https://live-server.bidobido.xyz"//新聊天室
    private var URL: String? = null //用于记录

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .dns(HttpDns())
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    val service: ApiService by lazy {
        getRetrofit(BASE_URL).create(ApiService::class.java)
    }

    val service_init: ApiService by lazy {
        getRetrofit(INFO).create(ApiService::class.java)
    }

    val service_update: ApiService by lazy {
        getRetrofit(UPDATE).create(ApiService::class.java)
    }

    val service_live: ApiService by lazy {
        getRetrofit(LIVE_SERVER).create(ApiService::class.java)
    }
    val kotlinJson = Json {
        ignoreUnknownKeys = true
    }

    private fun getRetrofit(url: String): Retrofit {
        if (retrofit == null || URL != url) {
            URL = url//记录baseurl
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(kotlinJson.asConverterFactory("application/json; charset=UTF8".toMediaType()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        }
        return retrofit!!
    }
}