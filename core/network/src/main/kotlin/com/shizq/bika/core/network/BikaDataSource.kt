package com.shizq.bika.core.network

import com.shizq.bika.core.network.model.CollectionsData
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.EpisodeData
import com.shizq.bika.core.network.model.KeywordsData
import com.shizq.bika.core.network.model.LoginData
import com.shizq.bika.core.network.model.NetworkBootstrapConfig
import com.shizq.bika.core.network.model.ProfileData
import com.shizq.bika.core.network.model.RecommendationData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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

    suspend fun getKeywords(): KeywordsData {
        return client.get("keywords").body()
    }

    suspend fun getComicDetails(id: String): ComicData {
        return client.get("comics/$id").body()
    }

    suspend fun getRecommendations(id: String): RecommendationData {
        return client.get("comics/$id/recommendation").body()
    }

    suspend fun getComicEpisodes(id: String, page: Int): EpisodeData {
        return client.get("comics/$id/eps") {
            parameter("page", page)
        }.body()
    }

    suspend fun toggleLike(id: String) {
        client.post("comics/$id/like")
    }

    suspend fun toggleFavourite(id: String) {
        client.post("comics/$id/favourite")
    }

    suspend fun getCollections(): CollectionsData {
        return client.get("collections").body()
    }
}