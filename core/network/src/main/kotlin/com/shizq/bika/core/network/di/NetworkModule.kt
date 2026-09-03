package com.shizq.bika.core.network.di

import android.content.Context
import androidx.tracing.trace
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BuildConfig
import com.shizq.bika.core.network.auth.SessionExpiryReason
import com.shizq.bika.core.network.auth.SessionManager
import com.shizq.bika.core.network.auth.sessionExpiryPlugin
import com.shizq.bika.core.network.plugin.ApiEnvelopePlugin
import com.shizq.bika.core.network.plugin.DirectDns
import com.shizq.bika.core.network.plugin.DomainFallbackInterceptor
import com.shizq.bika.core.network.plugin.bikaAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
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
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool = ConnectionPool(
        10,
        5,
        TimeUnit.MINUTES,
    )

    @Provides
    @Singleton
    fun providesHttpClient(
        okHttpClient: OkHttpClient,
        userCredentialsDataSource: UserCredentialsDataSource,
        userPreferencesDataSource: UserPreferencesDataSource,
        sessionManager: SessionManager,
    ): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        defaultRequest {
            url("https://picaapi.picacomic.com")
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }
        // 通路 A：HTTP 401。装在信封插件之前，先于响应体解析拦下鉴权失败
        install(sessionExpiryPlugin(sessionManager))
        install(ApiEnvelopePlugin) {
            // 通路 B：HTTP 200 + 信封内 code=401
            onUnauthorized {
                sessionManager.terminateSession(SessionExpiryReason.TokenRejected)
            }
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }
        // 仅 DEBUG 开启请求日志：LogLevel.ALL 会打印 Authorization token 与签名头，
        // 无条件启用会导致 release 包凭据泄露到 logcat
        if (BuildConfig.DEBUG) {
            Logging {
                logger = Logger.ANDROID
                level = LogLevel.ALL
            }
        }
        bikaAuth {
            channel {
                val activeLine = userPreferencesDataSource.userData.first().network.dns.activeLine
                when (activeLine.lowercase()) {
                    "telecom" -> "1"
                    "unicom" -> "2"
                    "mobile" -> "3"
                    else -> "1"
                }
            }
            token {
                userCredentialsDataSource.userData.firstOrNull()?.token
            }
        }
    }

    /**
     * API 链路的 OkHttpClient。
     *
     * 不再挂 `Authenticator`：401 的处理已上移到 Ktor 层的
     * [com.shizq.bika.core.network.auth.sessionExpiryPlugin]。
     * OkHttp 的 `Authenticator` 是同步回调，只能靠 `runBlocking` 桥接
     * suspend 的凭据读取与重登，会阻塞 OkHttp dispatcher 线程；并发 401 时
     * 多个线程互等且重登请求抢不到同 host 的请求配额，形成死锁。
     * Ktor 拦截器天生 suspend，不占请求配额，这类问题不复存在。
     */
    @Provides
    @Singleton
    fun okHttpCallFactory(
        connectionPool: ConnectionPool,
        directDns: DirectDns,
    ): OkHttpClient = trace("OkHttpClient") {
        OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .dns(directDns)
            .build()
    }

    /**
     * 图片链路专用 OkHttpClient，与 API 链路隔离。
     *
     * Coil 会并发加载几十张图，共用 API 客户端时图片请求会挤占同 host 的
     * 请求配额与连接池，拖慢接口响应。图片 401 通常源于签名或防盗链，
     * 也不该触发会话终止，所以这里刻意不装任何鉴权组件。
     */
    @Provides
    @Singleton
    @Named("image")
    fun imageOkHttpClient(
        directDns: DirectDns,
    ): OkHttpClient = trace("ImageOkHttpClient") {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES))
            .dns(directDns)
            .build()
    }

    @Provides
    @Singleton
    fun imageLoader(
        @Named("image") okHttpClient: OkHttpClient,
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

    @Provides
    @Singleton
    @Named("github")
    fun provideGithubHttpClient(
        connectionPool: ConnectionPool,
    ): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .build()
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.HEADERS
            }
        }
    }

    /**
     * DNS 解析专用 [HttpClient]：独立于主链路，不带鉴权、不走 DirectDns，
     * 避免"解析直连 IP 却依赖直连 IP"的循环依赖。短超时快速失败。
     */
    @Provides
    @Singleton
    @Named("dns")
    fun provideDnsHttpClient(
        connectionPool: ConnectionPool,
    ): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .build()
        }
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000L
            requestTimeoutMillis = 10_000L
            socketTimeoutMillis = 10_000L
        }
        install(ApiEnvelopePlugin)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }
    }
}
