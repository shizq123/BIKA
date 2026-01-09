package com.shizq.bika.network

import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.bean.ChatRoomBlackListBean
import com.shizq.bika.bean.ChatRoomBlackListDeleteBean
import com.shizq.bika.bean.ChatRoomBlockUserBean
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.bean.ChatRoomProfileBean
import com.shizq.bika.bean.ChatRoomSignInBean
import com.shizq.bika.bean.ComicListBean
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.bean.GameInfoBean
import com.shizq.bika.bean.GamesBean
import com.shizq.bika.bean.InitBean
import com.shizq.bika.bean.MyCommentsBean
import com.shizq.bika.bean.NotificationsBean
import com.shizq.bika.bean.ProfileBean
import com.shizq.bika.bean.ReportBean
import com.shizq.bika.bean.SignInBean
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

    //用户个人信息
    @GET("users/profile")
    fun profileGet(
        @HeaderMap headers: Map<String, String>
    ): Observable<BaseResponse<ProfileBean>>

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

    //随机漫画
    @GET("comics/random")
    suspend fun randomGet(
        @HeaderMap headers: Map<String, String>
    ): BaseResponse<ComicListBean2>

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