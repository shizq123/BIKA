package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.LoginData
import dagger.Lazy
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val TAG = "TokenAuthenticator"

class TokenAuthenticator @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val apiProvider: Lazy<BikaDataSource>
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "authenticate: 401 Unauthorized detected for request: ${response.request.url}")

        val userData = runBlocking { userCredentialsDataSource.userData.first() }
        val oldToken = userData.token

        if (oldToken == null || response.request.header("Authorization")
                ?.endsWith(oldToken) == false
        ) {
            Log.w(TAG, "authenticate: No token found or token mismatch. Aborting refresh.")
            return null
        }

        return synchronized(this) {
            Log.d(TAG, "authenticate: Entered synchronized block.")
            val latestUserData = runBlocking { userCredentialsDataSource.userData.first() }
            val latestToken = latestUserData.token

            if (oldToken != latestToken) {
                Log.i(
                    TAG,
                    "authenticate: Token was already refreshed by another thread. Retrying with the new token."
                )
                return@synchronized response.request.newBuilder()
                    .header("Authorization", latestToken!!)
                    .build()
            }

            val username = latestUserData.username
            val password = latestUserData.password
            if (username == null || password == null) {
                Log.e(TAG, "authenticate: Cannot refresh token, no username/password saved.")
                return@synchronized null
            }

            Log.i(TAG, "authenticate: Token expired. Attempting to re-login for user: '$username'")

            try {
                val loginData: LoginData = runBlocking {
                    apiProvider.get().login(username, password)
                }

                val newToken = loginData.token

                runBlocking { userCredentialsDataSource.setToken(newToken) }
                Log.i(TAG, "authenticate: Token refresh successful. New token saved.")

                Log.d(TAG, "authenticate: Retrying original request with new token.")
                return@synchronized response.request.newBuilder()
                    .header("Authorization", newToken)
                    .build()

            } catch (e: Exception) {
                Log.e(TAG, "authenticate: Token refresh failed due to an exception.", e)

                runBlocking { userCredentialsDataSource.setToken(null) }
                Log.w(TAG, "authenticate: Cleared local token due to refresh failure.")

                return@synchronized null
            }
        }
    }
}