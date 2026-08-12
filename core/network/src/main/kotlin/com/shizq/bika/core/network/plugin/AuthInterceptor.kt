package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.BuildConfig
import dagger.Lazy
import jakarta.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val TAG = "TokenAuthenticator"
private const val MAX_RETRY_COUNT = 2
// 等待刷新锁的超时：防止多个 401 回调线程互等导致连接池耗尽死锁
private const val MUTEX_WAIT_TIMEOUT_MS = 5_000L
private val DEBUG_LOGGING = BuildConfig.DEBUG

class TokenAuthenticator @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val authApiProvider: Lazy<BikaDataSource>
) : Authenticator {
    private val mutex = Mutex()
    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        val retryCount = response.responseCount()
        if (DEBUG_LOGGING) Log.i(
            TAG,
            "authenticate: received 401, path=$path, code=${response.code}, retryCount=$retryCount"
        )

        // 登录请求自身收到 401 时直接放弃，避免 authenticate 内再次发起 login
        // 又触发 401 重入，与外层 runBlocking + Mutex 互等形成死锁。
        if (path.contains("/auth/sign-in")) {
            if (DEBUG_LOGGING) Log.w(TAG, "authenticate: login request itself returned 401, give up. path=$path")
            return null
        }

        if (retryCount > MAX_RETRY_COUNT) {
            if (DEBUG_LOGGING) Log.e(
                TAG,
                "authenticate: max retry reached, give up. path=$path, retryCount=$retryCount"
            )
            return null
        }
        return runBlocking {
            val userData = userCredentialsDataSource.userData.first()
            val oldToken = userData.token
            val username = userData.username
            val password = userData.password
            if (DEBUG_LOGGING) Log.d(
                TAG,
                "authenticate: loaded credentials. username=${safeUsername(username)}, oldToken=${maskToken(oldToken)}"
            )
            if (oldToken == null || username.isNullOrBlank() || password.isNullOrBlank()  ) {
                if (DEBUG_LOGGING) Log.e(TAG, "authenticate: username/password/token is empty, cannot re-login.")
                return@runBlocking null
            }
            if (DEBUG_LOGGING) Log.d(TAG, "authenticate: waiting for mutex...")
            try {
                // 等锁加超时：多请求同时 401 时，等待线程会占用 OkHttp 连接池槽位，
                // 若槽位被占满则 login 永远拿不到连接，形成死锁。超时放弃本次重试即可打破。
                withTimeout(MUTEX_WAIT_TIMEOUT_MS) {
                    mutex.withLock {
                        if (DEBUG_LOGGING) Log.d(TAG, "authenticate: mutex acquired.")
                        val latestToken = userCredentialsDataSource.userData.first().token
                        if (DEBUG_LOGGING) Log.d(
                            TAG,
                            "authenticate: latest token from storage=${maskToken(latestToken)}, oldToken=${maskToken(oldToken)}"
                        )
                        if (!latestToken.isNullOrBlank() && latestToken != oldToken) {
                            if (DEBUG_LOGGING) Log.i(
                                TAG,
                                "authenticate: token already refreshed by another request, reuse it."
                            )
                            return@withLock response.request.newBuilder()
                                .header("Authorization", latestToken)
                                .build()
                        }
                        try {
                            if (DEBUG_LOGGING) Log.i(TAG, "authenticate: start re-login. path=$path")
                            val loginResult = authApiProvider.get().login(username, password)
                            val newToken = loginResult.token
                            if (DEBUG_LOGGING) Log.i(
                                TAG,
                                "authenticate: re-login finished. newToken=${maskToken(newToken)}"
                            )
                            if (newToken.isNullOrBlank()) {
                                if (DEBUG_LOGGING) Log.e(TAG, "authenticate: re-login succeeded but token is null/blank.")
                                return@withLock null
                            }
                            userCredentialsDataSource.setToken(newToken)
                            if (DEBUG_LOGGING) Log.i(TAG, "authenticate: token updated in storage.")
                            val newRequest = response.request.newBuilder()
                                .header("Authorization", newToken)
                                .build()
                            if (DEBUG_LOGGING) Log.i(TAG, "authenticate: rebuilt request with new token, retrying.")
                            newRequest
                        } catch (e: Exception) {
                            if (DEBUG_LOGGING) Log.e(TAG, "authenticate: re-login failed.", e)
                            null
                        } finally {
                            if (DEBUG_LOGGING) Log.d(TAG, "authenticate: leaving mutex block.")
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                if (DEBUG_LOGGING) Log.e(TAG, "authenticate: wait mutex timeout, give up to avoid deadlock.")
                null
            }
        }
    }

    private fun Response.responseCount(): Int {
        var count = 1
        var current = priorResponse
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }

    private fun maskToken(token: String?): String {
        if (token.isNullOrBlank()) return "null"
        return when {
            token.length <= 10 -> "***"
            else -> "${token.take(4)}...${token.takeLast(4)}"
        }
    }

    private fun safeUsername(username: String?): String {
        if (username.isNullOrBlank()) return "null"
        return if (username.length <= 3) "***" else "${username.take(3)}***"
    }
}