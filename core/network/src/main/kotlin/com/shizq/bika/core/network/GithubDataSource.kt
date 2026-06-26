package com.shizq.bika.core.network

import com.shizq.bika.core.network.model.GithubReleaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
class GithubDataSource @Inject constructor(
    @Named("github") private val httpClient: HttpClient,
) {
    suspend fun getLatestRelease(): GithubReleaseResponse {
        return httpClient
            .get("https://api.github.com/repos/STlxx-lin/BIKA/releases/tags/latest") {
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            .body()
    }
}
