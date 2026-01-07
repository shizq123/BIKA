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

class TokenAuthenticator @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val apiProvider: Lazy<BikaDataSource>
) : Authenticator {
    private val mutex = Mutex()
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "authenticate: 401 Unauthorized for ${response.request.url}")

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

                if (tokenBeforeRefresh != currentToken) {
                    Log.i(
                        TAG,
                        "authenticate: Token was already refreshed. Retrying with the new token."
                    )
                    return@withLock buildRequestWithNewToken(response.request, currentToken!!)
                }

                Log.i(TAG, "authenticate: Attempting to re-login for user: '$username'")
                try {
                    val loginData = apiProvider.get().login(username, password)
                    val newToken = loginData.token

                    userCredentialsDataSource.setToken(newToken)
                    Log.i(TAG, "authenticate: Token refresh successful. New token saved.")

                    return@withLock buildRequestWithNewToken(response.request, newToken)
                } catch (e: Exception) {
                    Log.e(TAG, "authenticate: Token refresh failed.", e)
                    userCredentialsDataSource.setToken(null)
                    return@withLock null
                }
            }
        }
    }

    private fun buildRequestWithNewToken(request: Request, token: String): Request {
        return request.newBuilder()
            .header("Authorization", token)
            .build()
    }
}