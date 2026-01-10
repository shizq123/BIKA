package com.shizq.bika.core.network

import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.model.ActionData
import com.shizq.bika.core.network.model.ChapterPagesData
import com.shizq.bika.core.network.model.CollectionsData
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicRandomData
import com.shizq.bika.core.network.model.ComicResource
import com.shizq.bika.core.network.model.CommentsData
import com.shizq.bika.core.network.model.EpisodeData
import com.shizq.bika.core.network.model.KeywordsData
import com.shizq.bika.core.network.model.KnightLeaderboardData
import com.shizq.bika.core.network.model.LeaderboardData
import com.shizq.bika.core.network.model.LoginData
import com.shizq.bika.core.network.model.NetworkBootstrapConfig
import com.shizq.bika.core.network.model.ProfileData
import com.shizq.bika.core.network.model.RecommendationData
import com.shizq.bika.core.network.model.Type
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

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

    suspend fun toggleComicLike(id: String): ActionData {
        return client.post("comics/$id/like").body()
    }

    suspend fun toggleComicFavourite(id: String): ActionData {
        return client.post("comics/$id/favourite").body()
    }

    suspend fun getCollections(): CollectionsData {
        return client.get("collections").body()
    }

    suspend fun getLeaderboard(timeType: String): LeaderboardData {
        return client.get("comics/leaderboard") {
            parameter("tt", timeType)
            parameter("ct", "VC")
        }.body()
    }

    suspend fun getKnightLeaderboard(): KnightLeaderboardData {
        return client.get("comics/knight-leaderboard").body()
    }

    suspend fun getChapterPages(id: String, order: Int, page: Int): ChapterPagesData {
        return client.get("comics/$id/order/$order/pages") {
            parameter("page", page)
        }.body<ChapterPagesData>()
    }

    // todo add type parameter
    suspend fun getComicComments(id: String, page: Int): CommentsData {
        return client.get("comics/$id/comments/") {
            parameter("page", page)
        }.body()
    }

    /**
     * 获取指定评论的回复列表
     */
    suspend fun getReplyReply(id: String, page: Int): CommentsData {
        return client.get("comments/$id/childrens/") {
            parameter("page", page)
        }.body()
    }

    suspend fun addReply(type: Type, id: String, content: String) {
        client.post("${type.type}/$id/comments") {
            setBody("""{"content":"$content"}""")
        }.bodyAsText()
    }

    suspend fun getGameComments(id: String, page: Int): CommentsData {
        return client.get("games/$id/comments/") {
            parameter("page", page)
        }.body()
    }

    /**
     * 切换主评论的点赞状态 (点赞/取消点赞)
     */
    suspend fun toggleCommentLike(id: String): ActionData {
        return client.post("comments/$id/like").body()
    }

    /**
     * 切换子评论（回复）的点赞状态 (点赞/取消点赞)
     */
    suspend fun toggleReplyLike(id: String): ActionData {
        return client.post("comments/$id/like").body()
    }

    suspend fun searchComics(
        topic: String? = null,
        tag: String? = null,
        authorName: String? = null,
        knightId: String? = null,
        translationTeam: String? = null,
        sort: Sort,
        page: Int,
    ): ComicResource {
        return client.get("comics") {
            parameter("c", topic)
            parameter("t", tag)
            parameter("a", authorName)
            parameter("ca", knightId)
            parameter("ct", translationTeam)
            parameter("s", sort)
            parameter("page", page)
        }.body()
    }

    suspend fun advancedSearch(
        content: String,
        categories: List<String>,
        sort: Sort,
        page: Int
    ): ComicResource {
        return client.post("comics/advanced-search") {
            val body = buildJsonObject {
                put("keyword", JsonPrimitive(content))
                put("sort", JsonPrimitive(sort.value))
                putJsonArray("categories") {
                    addAll(categories)
                }
            }
            parameter("page", page)
            setBody(body)
        }.body()
    }

    suspend fun getFavouriteComics(sort: Sort, page: Int): ComicResource {
        return client.get("users/favourite") {
            parameter("s", sort)
            parameter("page", page)
        }.body()
    }

    suspend fun getRandomComics(): ComicRandomData {
        return client.get("comics/random").body()
    }
}