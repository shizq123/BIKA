package com.shizq.bika.core.network.di

import android.content.Context
import androidx.tracing.trace
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.NetworkLine
import com.shizq.bika.core.network.BuildConfig
import com.shizq.bika.core.network.plugin.DomainFallbackInterceptor
import com.shizq.bika.core.network.plugin.ResponseTransformer
import com.shizq.bika.core.network.plugin.TokenAuthenticator
import com.shizq.bika.core.network.plugin.bikaAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient

// TODO: 一个暂时的操作，迁移后删除 
var ProjectOkhttp: OkHttpClient? = null
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun providesHttpClient(
        okHttpClient: OkHttpClient,
        userCredentialsDataSource: UserCredentialsDataSource,
        userPreferencesDataSource: UserPreferencesDataSource,
    ): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        defaultRequest {
            url("https://picaapi.picacomic.com")
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }
        install(ResponseTransformer)
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
        Logging {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
        bikaAuth {
            appChannel = runBlocking {
                when (userPreferencesDataSource.userData.first().selectedNetworkLine) {
                    NetworkLine.LINE_1 -> "1"
                    NetworkLine.LINE_2 -> "2"
                    NetworkLine.LINE_3 -> "3"
                }
            }
            token {
                userCredentialsDataSource.userData.firstOrNull()?.token
            }
        }
    }

    @Provides
    @Singleton
    fun okHttpCallFactory(
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = trace("OkHttpClient") {
        OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .dns {
                listOf("172.67.194.19", "104.21.20.188", "104.19.53.76")
                    .flatMap { Dns.SYSTEM.lookup(it) }
            }
            .build()
            .also { ProjectOkhttp = it }
    }

    @Provides
    @Singleton
    fun imageLoader(
        okHttpClient: OkHttpClient,
        @ApplicationContext application: Context,
    ): ImageLoader = trace("ImageLoader") {
        ImageLoader.Builder(application)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
                add(DomainFallbackInterceptor())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}