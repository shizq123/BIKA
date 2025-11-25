package com.shizq.bika.core.network.di

import android.content.Context
import androidx.tracing.trace
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.shizq.bika.core.network.BuildConfig
import com.shizq.bika.core.network.plugin.bikaAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun providesHttpClient(okHttpCallFactory: OkHttpClient): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpCallFactory
        }
        defaultRequest {
            url("https://picaapi.picacomic.com")
        }
        bikaAuth {

        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        Logging {
            logger = Logger.ANDROID
            level = LogLevel.BODY
        }
    }.apply {
        plugin(HttpSend).intercept { request ->
            val path = request.url.encodedPath
            val method = request.method.value

            val headerMap = PicaAuth.generate(path, method)

            headerMap.forEach { (key, value) ->
                request.header(key, value)
            }

            execute(request)
        }
    }

    @Provides
    @Singleton
    fun okHttpCallFactory(): OkHttpClient = trace("OkHttpClient") {
        OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun imageLoader(
        okHttpCallFactory: OkHttpClient,
        @ApplicationContext application: Context,
    ): ImageLoader = trace("ImageLoader") {
        ImageLoader.Builder(application)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpCallFactory))
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}