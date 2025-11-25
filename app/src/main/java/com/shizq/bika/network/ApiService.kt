package com.shizq.bika.network

import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.Box
import com.shizq.bika.bean.Catalogue
import com.shizq.bika.bean.CategoriesBean
import com.shizq.bika.bean.Category
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.bean.ChatRoomBlackListBean
import com.shizq.bika.bean.ChatRoomBlackListDeleteBean
import com.shizq.bika.bean.ChatRoomBlockUserBean
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.bean.ChatRoomListOldBean
import com.shizq.bika.bean.ChatRoomProfileBean
import com.shizq.bika.bean.ChatRoomSignInBean
import com.shizq.bika.bean.CollectionsBean
import com.shizq.bika.bean.ComicEpisodeData
import com.shizq.bika.bean.ComicInfoBean
import com.shizq.bika.bean.ComicListBean
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.bean.EpisodeBean
import com.shizq.bika.bean.GameInfoBean
import com.shizq.bika.bean.GamesBean
import com.shizq.bika.bean.InitBean
import com.shizq.bika.bean.KeywordsBean
import com.shizq.bika.bean.KnightBean
import com.shizq.bika.bean.MyCommentsBean
import com.shizq.bika.bean.NotificationsBean
import com.shizq.bika.bean.PicaAppsBean
import com.shizq.bika.bean.PicaInitBean
import com.shizq.bika.bean.ProfileBean
import com.shizq.bika.bean.PunchInBean
import com.shizq.bika.bean.RecommendBean
import com.shizq.bika.bean.ReportBean
import com.shizq.bika.bean.SignInBean
import com.shizq.bika.bean.UserProfile
import com.shizq.bika.network.base.BaseResponse
import io.reactivex.rxjava3.core.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    //节点
    @GET("init")
    fun initGet(): Observable<InitBean>

    //平台 info 不清楚用途
    @GET("init")
    fun picaInitGet(
        @Query("platform") platform: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<PicaInitBean>

    //用户个人信息
    @GET("users/profile")
    fun profileGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ProfileBean>>

    @GET("users/profile")
    suspend fun profileGet2(
        @HeaderMap headers: Map<String, String>
    ): Box<UserProfile>

    //打卡签到
    @POST("users/punch-in")
    fun punchInPOST(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<PunchInBean>>

    //登录
    @POST("auth/sign-in")
    fun signInPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<SignInBean>>

    //注册
    @POST("auth/register")
    fun signUpPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<SignInBean>>

    //忘记密码
    @POST("auth/forgot-password")
    fun forgotPasswordPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<SignInBean>>

    //忘记密码 获得密码
    @POST("auth/reset-password")
    fun resetPasswordPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<SignInBean>>

    //修改密码
    @PUT("users/password")
    fun changePasswordPUT(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<Any>>

    //首页推荐
    @GET("collections")
    suspend fun collectionsGet(
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<CollectionsBean>

    //分类
    @GET("categories")
    fun categoriesGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<CategoriesBean>>

    @GET("categories")
    suspend fun categoriesGet2(
        @HeaderMap headers: Map<String, String>
    ): Box<Category>

    //骑士榜
    @GET("comics/knight-leaderboard")
    fun knightGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<KnightBean>>

    //热门榜 tt=H24 D7 D30
    @GET("comics/leaderboard")
    fun leaderboardGet(
        @Query("tt") tt: String,
        @Query("ct") ct: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ComicListBean2>>

    //漫画列表
    @GET("comics")
    suspend fun comicsListGet(
        @Query("page") page: String,
        @Query("c") name: String,
        @Query("s") type: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //漫画列表
    @GET("comics")
    suspend fun comicsListGet(
        @Query("page") page: String,
        @Query("s") type: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //搜索漫画列表
    @POST("comics/advanced-search")
    suspend fun comicsSearchPOST(
        @Query("page") page: Int,
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //搜索 热搜词条
    @GET("keywords")
    fun keywordsGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<KeywordsBean>>

    //随机漫画
    @GET("comics/random")
    suspend fun randomGet(
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean2>

    //漫画详情
    @GET("comics/{bookId}")
    suspend fun comicsInfoGet(
        @Path("bookId") bookId: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicInfoBean>

    //漫画详情下面的推荐漫画
    @GET("comics/{bookId}/recommendation")
    suspend fun recommendGet(
        @Path("bookId") bookId: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<RecommendBean>

    //漫画 喜欢
    @POST("comics/{bookId}/like")
    suspend fun comicsLikePost(
        @Path("bookId") bookId: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ActionBean>

    //漫画 收藏
    @POST("comics/{bookId}/favourite")
    suspend fun comicsFavouritePost(
        @Path("bookId") bookId: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ActionBean>

    //漫画章节
    @GET("comics/{bookId}/eps")
    suspend fun comicsEpisodeGet(
        @Path("bookId") bookId: String,
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<EpisodeBean>

    //漫画章节
    @GET("comics/{bookId}/eps")
    suspend fun comicsEpisodeGet2(
        @Path("bookId") id: String,
        @Query("page") page: Int,
        @HeaderMap headers: Map<String, String>
    ): Box<Catalogue>

    @GET("comics/{bookId}/order/{order}/pages")
    suspend fun comicsPictureGet2(
        @Path("bookId") bookId: String,
        @Path("order") order: Int,
        @Query("page") page: Int,
        @HeaderMap headers: Map<String, String>
    ): Box<ComicEpisodeData>

    //评论列表
    @GET("{comics_games}/{id}/comments")
    suspend fun comicsCommentsGet(
        @Path("comics_games") comics_games: String,
        @Path("id") id: String,
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<CommentsBean>

    //发送评论 游戏与漫画的评论
    @POST("{comics_games}/{id}/comments")
    fun seedCommentsPost(
        @Path("comics_games") comics_games: String,
        @Path("id") id: String,
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<Any>>

    //子评论列表
    @GET("comments/{commentsId}/childrens")
    suspend fun commentsChildrensGet(
        @Path("commentsId") commentsId: String,
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<CommentsBean>

    //发送子评论
    @POST("comments/{id}")
    fun seedSubCommentsPost(
        @Path("id") id: String,
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<Any>>

    //评论 like 喜欢
    @POST("comments/{id}/like")
    fun commentsLikePost(
        @Path("id") id: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ActionBean>>

    //评论 举报
    @POST("comments/{id}/report")
    fun commentsReportPost(
        @Path("id") id: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ReportBean>>

    //漫画详细 搜索 标签
    @GET("comics")
    suspend fun tagsGet(
        @Query("page") page: String,
        @Query("t") tagsName: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //漫画详细 搜索 汉化组
    @GET("comics")
    suspend fun translateGet(
        @Query("page") page: String,
        @Query("ct") translate: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //漫画详细 搜索 作者
    @GET("comics")
    suspend fun authorGet(
        @Query("page") page: String,
        @Query("a") author: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //漫画详细 搜索 创作者
    @GET("comics")
    suspend fun creatorGet(
        @Query("page") page: String,
        @Query("ca") creator: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //s=dd新到旧,da旧到新,ld最多爱心,vd最多指名
    @GET("users/favourite")
    suspend fun myFavouriteGet(
        @Query("s") s: String,
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean>

    //查看用户信息
    @GET("users/{userId}/profile")
    fun userProfileGet(
        @Path("userId") userId: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ProfileBean>>

    //游戏列表
    @GET("games")
    fun gamesGet(
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<GamesBean>>

    //游戏详情
    @GET("games/{gameId}")
    fun gameInfoGet(
        @Path("gameId") gameId: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<GameInfoBean>>

    //游戏喜欢
    @POST("games/{gameId}/like")
    fun gameLikePost(
        @Path("gameId") gameId: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ActionBean>>

    //我的评论
    @GET("users/my-comments")
    fun myCommentsGet(
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<MyCommentsBean>>

    //消息通知
    @GET("users/notifications")
    fun notificationsGet(
        @Query("page") page: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<NotificationsBean>>

    //小程序列表
    @GET("pica-apps")
    suspend fun picaAppsGet(
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<PicaAppsBean>

    //聊天室列表
    @GET("chat")
    suspend fun oldChatRoomListGet(
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ChatRoomListOldBean>

    //上传头像 //抓包得出上传的分辨率是200*200 //聊天室抓包，图片其中一边的最大分辨率是800
    @PUT("users/avatar")
    fun avatarPUT(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<Any>>

    //上传自我介绍 签名
    @PUT("users/profile")
    fun profilePUT(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<Any>>

    //新聊天室 登录
    @POST("auth/signin")
    suspend fun chatSignInPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): ChatRoomSignInBean

    //新聊天室 用户信息
    @GET("user/profile")
    fun chatProfileGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<ChatRoomProfileBean>

    //新聊天室 查看别人的用户信息
    @GET("user/profile/{id}")
    fun chatUserProfileGet(
        @Query("id") page: String,
        @HeaderMap headers: Map<String, String>
    ): Observable<ChatRoomProfileBean>

    //新聊天室 房间列表
    @GET("room/list")
    suspend fun chatRoomListGet(
        @HeaderMap headers: Map<String, String>
    ): ChatRoomListBean

    //新聊天室 封锁用户 添加黑名单 https://live-server.bidobido.xyz/blacklist/block-user 请求文本{"userId":"xxx"}
    @POST("blacklist/block-user")
    fun chatBlockUserPost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<ChatRoomBlockUserBean>

    //新聊天室 封锁列表 黑名单列表 https://live-server.bidobido.xyz/blacklist/list?offset=0
    @GET("blacklist/list")
    suspend fun chatBlackListGet(
        @Query("offset") page: Int,
        @HeaderMap headers: Map<String, String>
    ): ChatRoomBlackListBean

    //新聊天室 删除封锁 删除黑名单 https://live-server.bidobido.xyz/blacklist/63e3373e4548486c28e60cc5
    @DELETE("blacklist/{id}")
    suspend fun chatBlackListDelete(
        @Path("id") gameId: String,
        @HeaderMap headers: Map<String, String>
    ): ChatRoomBlackListDeleteBean

    //新聊天室 发送消息
    @POST("room/send-message")
    fun chatSendMessagePost(
        @Body requestBody: RequestBody,
        @HeaderMap headers: Map<String, String>
    ): Observable<ChatMessageBean>

    //新聊天室 发送图片
    @POST("room/send-image")
    fun chatSendImagePost(
        @HeaderMap headers: Map<String, String>,
        @Body requestBody: RequestBody
    ): Observable<ChatMessageBean>
}