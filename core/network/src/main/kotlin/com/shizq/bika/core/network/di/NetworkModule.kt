package com.shizq.bika.core.network.di

import android.content.Context
import androidx.tracing.trace
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.util.DebugLogger
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BuildConfig
import com.shizq.bika.core.network.plugin.AuthInterceptor
import com.shizq.bika.core.network.plugin.ResponseTransformer
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
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun providesHttpClient(
        okHttpClient: OkHttpClient,
        userCredentialsDataSource: UserCredentialsDataSource,
    ): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        defaultRequest {
            url("https://picaapi.picacomic.com")
        }
        bikaAuth {
            token {
                userCredentialsDataSource.userData.first().token
            }
        }
        install(ResponseTransformer)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
                ContentType.Application.Json.withCharset(Charsets.UTF_8)
            )
        }
        Logging {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
    }

    @Provides
    @Singleton
    fun okHttpCallFactory(): OkHttpClient = trace("OkHttpClient") {
        OkHttpClient.Builder()
            .dns {
                Dns.SYSTEM.lookup("104.19.53.76")
            }
            .build()
    }

    @Provides
    @Singleton
    fun imageLoader(
        okHttpCallFactory: OkHttpClient,
        @ApplicationContext application: Context,
        authInterceptor: AuthInterceptor
    ): ImageLoader = trace("ImageLoader") {
        ImageLoader.Builder(application)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpCallFactory))
//                add(authInterceptor)
            }

            .apply {
                if (BuildConfig.DEBUG) {
                    networkCachePolicy(CachePolicy.DISABLED)
                    logger(DebugLogger())
                }
            }
            .build()
    }
}