package com.shizq.bika.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class BikaDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getCategories() {
        client.get("categories")
    }
}