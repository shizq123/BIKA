package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.Lazy
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val TAG = "TokenAuthenticator"
private const val MAX_RETRY_COUNT = 2

class TokenAuthenticator @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val authApiProvider: Lazy<BikaDataSource>
) : Authenticator {
    private val mutex = Mutex()
    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        val retryCount = response.responseCount()
        Log.i(
            TAG,
            "authenticate: received 401, path=$path, code=${response.code}, retryCount=$retryCount"
        )

        if (retryCount > MAX_RETRY_COUNT) {
            Log.e(
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
            Log.d(
                TAG,
                "authenticate: loaded credentials. username=${safeUsername(username)}, oldToken=${
                    maskToken(
                        oldToken
                    )
                }"
            )
            if (username.isNullOrBlank() || password.isNullOrBlank()) {
                Log.e(TAG, "authenticate: username/password is empty, cannot re-login.")
                return@runBlocking null
            }
            Log.d(TAG, "authenticate: waiting for mutex...")
            mutex.withLock {
                Log.d(TAG, "authenticate: mutex acquired.")
                val latestToken = userCredentialsDataSource.userData.first().token
                Log.d(
                    TAG,
                    "authenticate: latest token from storage=${maskToken(latestToken)}, oldToken=${
                        maskToken(
                            oldToken
                        )
                    }"
                )
                if (!latestToken.isNullOrBlank() && latestToken != oldToken) {
                    Log.i(
                        TAG,
                        "authenticate: token already refreshed by another request, reuse it."
                    )
                    return@withLock response.request.newBuilder()
                        .header("Authorization", latestToken)
                        .build()
                }
                try {
                    Log.i(TAG, "authenticate: start re-login. path=$path")
                    val loginResult = authApiProvider.get().login(username, password)
                    val newToken = loginResult.token
                    Log.i(
                        TAG,
                        "authenticate: re-login finished. newToken=${maskToken(newToken)}"
                    )
                    if (newToken.isNullOrBlank()) {
                        Log.e(TAG, "authenticate: re-login succeeded but token is null/blank.")
                        return@withLock null
                    }
                    userCredentialsDataSource.setToken(newToken)
                    Log.i(TAG, "authenticate: token updated in storage.")
                    val newRequest = response.request.newBuilder()
                        .header("Authorization", newToken)
                        .build()
                    Log.i(TAG, "authenticate: rebuilt request with new token, retrying.")
                    newRequest
                } catch (e: Exception) {
                    Log.e(TAG, "authenticate: re-login failed.", e)
                    null
                } finally {
                    Log.d(TAG, "authenticate: leaving mutex block.")
                }
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