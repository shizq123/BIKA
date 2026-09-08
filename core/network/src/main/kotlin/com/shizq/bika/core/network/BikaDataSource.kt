package com.shizq.bika.core.network

import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.auth.SkipSessionExpiry
import com.shizq.bika.core.network.model.ActionData
import com.shizq.bika.core.network.model.ChapterPagesData
import com.shizq.bika.core.network.model.CollectionsData
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicRandomData
import com.shizq.bika.core.network.model.ComicResource
import com.shizq.bika.core.network.model.CommentDoc
import com.shizq.bika.core.network.model.CommentsData
import com.shizq.bika.core.network.model.EpisodeData
import com.shizq.bika.core.network.model.KeywordsData
import com.shizq.bika.core.network.model.KnightLeaderboardData
import com.shizq.bika.core.network.model.LeaderboardData
import com.shizq.bika.core.network.model.LoginResult
import com.shizq.bika.core.network.model.LoginTokenPayload
import com.shizq.bika.core.network.model.NetworkBootstrapConfig
import com.shizq.bika.core.network.model.NotificationsData
import com.shizq.bika.core.network.model.ProfileData
import com.shizq.bika.core.network.model.RecommendationData
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.plugin.ApiException
import com.shizq.bika.core.network.plugin.ExpectRawResponse
import com.shizq.bika.core.network.plugin.UnauthorizedException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger("BikaNetwork")

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

    suspend fun getBootstrapConfig(): NetworkBootstrapConfig {
        val config = getBootstrapConfigWithHttpFallback()
        // 明文通道下的响应校验：过滤非法地址，全部非法时返回空列表（调用方不会更新 DNS）
        val validated = config.addresses.filter { it.isValidAddress() }
        if (validated.size != config.addresses.size) {
            logger.warn { "引导配置包含非法地址，已过滤: ${config.addresses.filterNot { it.isValidAddress() }}" }
        }
        return config.copy(addresses = validated)
    }

    /** 私有实现的核心信息是"带明文回退"，命名里写出来，别只留在 get 那层的注释里。 */
    private suspend fun getBootstrapConfigWithHttpFallback(): NetworkBootstrapConfig {
        return try {
            // 优先 HTTPS（防篡改）；服务器不支持时回退明文通道
            client.get("https://$BOOTSTRAP_HOST/init") {
                attributes.put(ExpectRawResponse, Unit)
                // 引导接口是匿名的，其 401 与用户会话无关
                attributes.put(SkipSessionExpiry, Unit)
            }.body()
        } catch (_: Exception) {
            client.get("http://$BOOTSTRAP_HOST/init") {
                attributes.put(ExpectRawResponse, Unit)
                attributes.put(SkipSessionExpiry, Unit)
            }.body()
        }
    }

    private fun String.isValidAddress(): Boolean = ADDRESS_PATTERN.matches(this)

    suspend fun login(username: String, password: String): LoginResult {
        return try {
            val payload = client.post("auth/sign-in") {
                // 登录接口的 401/其他业务错误码表示"本次账号密码不对"，是登录表单的
                // 业务错误，不能触发全局会话终止——此时本就没有会话可终止。
                attributes.put(SkipSessionExpiry, Unit)
                val jsonBody = buildJsonObject {
                    put("email", JsonPrimitive(username))
                    put("password", JsonPrimitive(password))
                }
                setBody(jsonBody)
            }.body<LoginTokenPayload>()
            LoginResult.Success(payload.token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            LoginResult.Rejected(e.message ?: "用户名或密码错误")
        } catch (e: ApiException) {
            LoginResult.Rejected(e.serverMessage)
        } catch (e: Exception) {
            LoginResult.Rejected(e.message ?: "登录失败")
        }
    }

    suspend fun punchIn() {
        client.post("users/punch-in").body<Unit>()
    }

    suspend fun updateUserProfileSlogan(slogan: String) {
        client.put("users/profile") {
            val jsonBody = buildJsonObject {
                put("slogan", JsonPrimitive(slogan))
            }
            setBody(jsonBody)
        }.body<Unit>()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        client.put("users/password") {
            val jsonBody = buildJsonObject {
                put("old_password", JsonPrimitive(oldPassword))
                put("password", JsonPrimitive(newPassword))
            }
            setBody(jsonBody)
        }.body<Unit>()
    }

    suspend fun fetchUserProfile(): ProfileData {
        return client.get("users/profile").body()
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
     * 获取指定评论的子评论（回复）列表。
     *
     * 原名 getReplyReply（"回复的回复"？）必须靠注释才能看懂；改名后语义自解释。
     */
    suspend fun getCommentReplies(commentId: String, page: Int): CommentsData {
        return client.get("comments/$commentId/childrens/") {
            parameter("page", page)
        }.body()
    }

    /**
     * 给漫画/游戏发一条主评论。
     *
     * 原名 addReply 与真正的"回复评论"（[postCommentReply]）只差两个词，
     * 参数又都是 (String, String)，传错了编译器不会拦；改名后两者不再可能混淆。
     */
    suspend fun postComment(type: Type, targetId: String, content: String) {
        client.post("${type.type}/$targetId/comments") {
            val jsonBody = buildJsonObject {
                put("content", JsonPrimitive(content))
            }
            setBody(jsonBody)
        }.body<Unit>()
    }

    /** 回复某条已有评论（原名 addCommentReply，见 [postComment] 的重命名说明）。 */
    suspend fun postCommentReply(commentId: String, content: String) {
        client.post("comments/$commentId") {
            val jsonBody = buildJsonObject {
                put("content", JsonPrimitive(content))
            }
            setBody(jsonBody)
        }.body<Unit>()
    }

    /**
     * 切换主评论的点赞状态 (点赞/取消点赞)
     */
    suspend fun toggleCommentLike(id: String): ActionData {
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
     * 注册新账号。原名 requestSignUp(obj: JsonObject) 的签名等于没有签名——
     * 调用方必须去翻实现才知道服务端要哪些字段；改成命名参数后接口自己说明需求。
     *
     * 失败响应示例：
     *  {
     *     "code": 400,
     *     "error": "1008",
     *     "message": "email is already exist"
     *   }
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        birthday: String,
        gender: String,
        question1: String,
        answer1: String,
        question2: String,
        answer2: String,
        question3: String,
        answer3: String,
    ): JsonObject {
        val body = buildJsonObject {
            put("email", JsonPrimitive(email))
            put("password", JsonPrimitive(password))
            put("name", JsonPrimitive(name))
            put("birthday", JsonPrimitive(birthday))
            put("gender", JsonPrimitive(gender))
            put("question1", JsonPrimitive(question1))
            put("answer1", JsonPrimitive(answer1))
            put("question2", JsonPrimitive(question2))
            put("answer2", JsonPrimitive(answer2))
            put("question3", JsonPrimitive(question3))
            put("answer3", JsonPrimitive(answer3))
        }
        return client.post("auth/register") {
            attributes.put(ExpectRawResponse, Unit)
            setBody(body)
        }.body()
    }

    /** 获取当前用户发表过的评论列表（原名 mineComment 是名词短语，不是动作，且 "mine" 有"挖矿"的歧义）。 */
    suspend fun getMyComments(page: Int): CommentDoc {
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