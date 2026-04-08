package com.shizq.bika.core.network

import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.model.ActionData
import com.shizq.bika.core.network.model.ChapterPagesData
import com.shizq.bika.core.network.model.CollectionsData
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicRandomData
import com.shizq.bika.core.network.model.ComicResource
import com.shizq.bika.core.network.model.CommentDoc
import com.shizq.bika.core.network.model.CommentsData
import com.shizq.bika.core.network.model.EpisodeData
import com.shizq.bika.core.network.model.GameData
import com.shizq.bika.core.network.model.GameDetailsDataa
import com.shizq.bika.core.network.model.KeywordsData
import com.shizq.bika.core.network.model.KnightLeaderboardData
import com.shizq.bika.core.network.model.LeaderboardData
import com.shizq.bika.core.network.model.LoginData
import com.shizq.bika.core.network.model.NetworkBootstrapConfig
import com.shizq.bika.core.network.model.ProfileData
import com.shizq.bika.core.network.model.RecommendationData
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.plugin.ExpectRawResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import com.shizq.bika.core.network.model.Result
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class BikaDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getNetworkConfig(): NetworkBootstrapConfig {
        return client.get("http://68.183.234.72/init") {
            attributes.put(ExpectRawResponse, Unit)
        }.body()
    }

    suspend fun login(username: String, password: String): Result<LoginData> {
        return try {
            val response = client.post("auth/sign-in") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$username","password":"$password"}""")
            }

            val json = response.readRawBytes().decodeToString()
            val jsonObj = Json.decodeFromString(JsonObject.serializer(), json)
            val code = jsonObj["code"]?.jsonPrimitive?.intOrNull

            when (code) {
                200 -> {
                    val dataObj =
                        jsonObj["data"]?.jsonObject ?: return Result.ErrorMessage("数据异常")
                    val token = dataObj["token"]?.jsonPrimitive?.content
                        ?: return Result.ErrorMessage("Token为空")
                    Result.Success(LoginData(token))
                }

                else -> {
                    val msg = jsonObj["message"]?.jsonPrimitive?.content ?: "请求失败"
                    Result.ErrorMessage(
                        when (msg) {
                            "invalid email or password" -> "用户名或密码错误"
                            else -> "登录失败"
                        }
                    )
                }
            }
        } catch (e: Exception) {
            // 网络错误、解析错误
            Result.ErrorMessage( "网络异常，请重试")
        }
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
        sort: Sort?,
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

    /**
     *  {
     *     "code": 400,
     *     "error": "1008",
     *     "message": "email is already exist"
     *   }
     */
    suspend fun requestSignUp(obj: JsonObject): JsonObject {
        return client.post("auth/register") {
            attributes.put(ExpectRawResponse, Unit)
            setBody(obj)
        }.body()
    }

    suspend fun getGameList(page: Int): GameData {
        return client.get("games") {
            parameter("page", page)
        }.body()
    }

    suspend fun getGameDetail(id: String): GameDetailsDataa {
        return client.get("games/$id").body()
    }

    suspend fun mineComment(page: Int): CommentDoc {
        return client.get("users/my-comments") {
            parameter("page", page)
        }.body()
    }
}