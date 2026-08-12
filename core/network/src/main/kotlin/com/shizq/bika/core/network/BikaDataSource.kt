package com.shizq.bika.core.network

import com.shizq.bika.core.model.SortOrder
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
import com.shizq.bika.core.network.model.NotificationsData
import com.shizq.bika.core.network.model.ProfileData
import com.shizq.bika.core.network.model.RecommendationData
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.plugin.ExpectRawResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray

@Singleton
class BikaDataSource @Inject constructor(
    private val client: HttpClient,
) {
    private companion object {
        const val BOOTSTRAP_HOST = "68.183.234.72"
        // 引导接口可能走明文 HTTP 回退，响应内容必须严格校验：
        // 仅接受合法 IP/主机名，防止中间人篡改注入恶意地址
        private val ADDRESS_PATTERN = Regex("^[0-9a-zA-Z.\\-:\\[\\]]{1,255}$")
    }

    suspend fun getNetworkConfig(): NetworkBootstrapConfig {
        val config = fetchBootstrapConfig()
        // 明文通道下的响应校验：过滤非法地址，全部非法时返回空列表（调用方不会更新 DNS）
        val validated = config.addresses.filter { it.isValidAddress() }
        if (validated.size != config.addresses.size) {
            android.util.Log.w("BikaNetwork", "引导配置包含非法地址，已过滤: ${config.addresses.filterNot { it.isValidAddress() }}")
        }
        return config.copy(addresses = validated)
    }

    private suspend fun fetchBootstrapConfig(): NetworkBootstrapConfig {
        return try {
            // 优先 HTTPS（防篡改）；服务器不支持时回退明文通道
            client.get("https://$BOOTSTRAP_HOST/init") {
                attributes.put(ExpectRawResponse, Unit)
            }.body()
        } catch (_: Exception) {
            client.get("http://$BOOTSTRAP_HOST/init") {
                attributes.put(ExpectRawResponse, Unit)
            }.body()
        }
    }

    private fun String.isValidAddress(): Boolean = ADDRESS_PATTERN.matches(this)

    suspend fun login(username: String, password: String): LoginData {
        return try {
            val response = client.post("auth/sign-in") {
                attributes.put(ExpectRawResponse, Unit)
                val jsonBody = buildJsonObject {
                    put("email", JsonPrimitive(username))
                    put("password", JsonPrimitive(password))
                }
                setBody(jsonBody)
            }

            val jsonObj = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val code = jsonObj["code"]?.jsonPrimitive?.int

            if (code == 200) {
                val dataObj = jsonObj["data"]?.jsonObject ?: throw Exception("数据异常")
                val token = dataObj["token"]?.jsonPrimitive?.content ?: throw Exception("Token为空")
                LoginData(token = token, message = null)
            } else {
                val msg = jsonObj["message"]?.jsonPrimitive?.content ?: "请求失败"
                LoginData(token = null, message = msg)
            }
        } catch (e: Exception) {
            LoginData(token = null, message = e.message ?: "登录失败")
        }
    }

    suspend fun punchIn() {
        client.post("users/punch-in").bodyAsText()
    }

    suspend fun updateUserProfileSlogan(slogan: String) {
        client.put("users/profile") {
            val jsonBody = buildJsonObject {
                put("slogan", JsonPrimitive(slogan))
            }
            setBody(jsonBody)
        }.bodyAsText()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        client.put("users/password") {
            val jsonBody = buildJsonObject {
                put("old_password", JsonPrimitive(oldPassword))
                put("password", JsonPrimitive(newPassword))
            }
            setBody(jsonBody)
        }.bodyAsText()
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

    suspend fun getComments(type: Type, id: String, page: Int): CommentsData {
        return client.get("${type.type}/$id/comments/") {
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
            val jsonBody = buildJsonObject {
                put("content", JsonPrimitive(content))
            }
            setBody(jsonBody)
        }.bodyAsText()
    }

    suspend fun addCommentReply(commentId: String, content: String) {
        client.post("comments/$commentId") {
            val jsonBody = buildJsonObject {
                put("content", JsonPrimitive(content))
            }
            setBody(jsonBody)
        }.bodyAsText()
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
        sort: SortOrder?,
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

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun advancedSearch(
        content: String,
        categories: List<String>,
        sort: SortOrder,
        page: Int
    ): ComicResource {
        val processedContent = if (content.trim().equals("小學女生(JS)", ignoreCase = true) ||
            content.trim().equals("小学女生(JS)", ignoreCase = true)
        ) {
            content.trimEnd() + "   "
        } else {
            content
        }

        return client.post("comics/advanced-search") {
            val body = buildJsonObject {
                put("keyword", JsonPrimitive(processedContent))
                put("sort", JsonPrimitive(sort.value))
                putJsonArray("categories") {
                    addAll(categories)
                }
            }
            parameter("page", page)
            setBody(body)
        }.body()
    }

    suspend fun getFavouriteComics(sort: SortOrder, page: Int): ComicResource {
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

    suspend fun getNotifications(page: Int): NotificationsData {
        return client.get("users/notifications") {
            parameter("page", page)
        }.body()
    }
}