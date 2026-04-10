package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.BuildConfig
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
    private val apiProvider: Lazy<BikaDataSource>
) : Authenticator {
    private val mutex = Mutex()
    override fun authenticate(route: Route?, response: Response): Request? {
        val retryCount = response.responseCount()
        if (retryCount > MAX_RETRY_COUNT) {
            Log.e(TAG, "authenticate: Max retry count reached ($MAX_RETRY_COUNT), giving up.")
            return null
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "authenticate: 401 Unauthorized for ${response.request.url}")
        }

        return runBlocking {
            val userData = userCredentialsDataSource.userData.first()
            val tokenBeforeRefresh = userData.token

            val username = userData.username
            val password = userData.password
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e(TAG, "authenticate: Cannot refresh token, no credentials saved.")
                return@runBlocking null
            }

            mutex.withLock {
                val currentToken = userCredentialsDataSource.userData.first().token
                if (tokenBeforeRefresh != currentToken && currentToken != null) {
                    Log.i(
                        TAG,
                        "authenticate: Token was already refreshed. Retrying with the new token."
                    )
                    return@withLock buildRequestWithNewToken(response.request, currentToken)
                }
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "authenticate: Attempting to re-login for user: '$username'")
                }
                try {
                    val result = apiProvider.get().login(username, password)
                    val newToken = result.token
                    if (newToken == null) {
                        Log.w(TAG, "authenticate: Login succeeded but token is null.")
                        return@withLock null
                    }
                    userCredentialsDataSource.setToken(newToken)
                    buildRequestWithNewToken(response.request, newToken)
                } catch (e: Exception) {
                    Log.e(TAG, "authenticate: Token refresh failed.", e)
                    userCredentialsDataSource.setToken(null)
                    null
                }
            }
        }
    }

    private fun buildRequestWithNewToken(request: Request, token: String): Request {
        return request.newBuilder()
            .header("Authorization", token)
            .build()
    }
    private fun Response.responseCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}