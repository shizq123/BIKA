package com.shizq.bika.core.network

import com.shizq.bika.core.network.model.LoginData
import com.shizq.bika.core.network.model.NetworkBootstrapConfig
import com.shizq.bika.core.network.model.ProfileData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class BikaDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun fetchInitConfig(): NetworkBootstrapConfig {
        return client.get("init").body()
    }

    suspend fun login(username: String, password: String): LoginData {
        return client.post("auth/sign-in") {
            setBody("""{"email":"$username","password":"$password"}""")
        }.body()
    }

    suspend fun punchIn() {
        client.post("users/punch-in").bodyAsText()
    }

    suspend fun fetchUserProfile(): ProfileData {
        return client.get("users/profile").body()
    }

    suspend fun fetchCategories() {
        client.get("categories")
    }
}