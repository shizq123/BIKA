package com.shizq.bika.core.network

import io.ktor.client.HttpClient
import jakarta.inject.Inject

class BikaDataSource @Inject constructor(
    private val client: HttpClient,
) {
}