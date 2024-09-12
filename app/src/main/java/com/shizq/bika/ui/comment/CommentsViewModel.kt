package com.shizq.bika.ui.comment

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.bean.ReportBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class CommentsViewModel(application: Application) : BaseViewModel(application) {
    var data: CommentsBean.Comments.Doc?=null
    var id =""//漫画或游戏的id
    var comicsOrGames  = ""//类型是漫画还是游戏
    var startpage = 0//主评论起始页数，用于跳转页数后判断当前页数
    var commentsPage = 0//主评论当前页数
    var pages = 1//主评论总页数
    var limit = 20//主评论每页显示多少
    var commentId = ""//评论id
    var subCommentsPage = 0//子评论当前页数
    var likePosition = -1
    var likeSubPosition = -1
    var likeCommentsId: String? = null
    var likeSubCommentsId: String? = null

    private val repository = CommentsRepository()

    //评论列表
    private val _comments = MutableStateFlow<Result<CommentsBean>?>(null)
    val comments: StateFlow<Result<CommentsBean>?> = _comments

    //子评论列表
    private val _subComments = MutableStateFlow<Result<CommentsBean>?>(null)
    val subComments: StateFlow<Result<CommentsBean>?> = _subComments

    //发送评论
    private val _seedComment = MutableStateFlow<Result<CommentsBean>?>(null)
    val seedComment: StateFlow<Result<CommentsBean>?> = _seedComment

    //回复评论
    private val _replyComment = MutableStateFlow<Result<CommentsBean>?>(null)
    val replyComment: StateFlow<Result<CommentsBean>?> = _replyComment

    //喜欢这条评论
    private val _likeComment = MutableStateFlow<Result<CommentsBean>?>(null)
    val likeComment: StateFlow<Result<CommentsBean>?> = _likeComment

    //喜欢这条子评论
    private val _likeSubComment = MutableStateFlow<Result<CommentsBean>?>(null)
    val likeSubComment: StateFlow<Result<CommentsBean>?> = _likeSubComment

    //举报评论
    private val _reportComment = MutableStateFlow<Result<CommentsBean>?>(null)
    val reportComment: StateFlow<Result<CommentsBean>?> = _reportComment


//    val liveData_comments: MutableLiveData<BaseResponse<CommentsBean>> by lazy {
//        MutableLiveData<BaseResponse<CommentsBean>>()
//    }

    val liveData_sub_comments: MutableLiveData<BaseResponse<CommentsBean>> by lazy {
        MutableLiveData<BaseResponse<CommentsBean>>()
    }

    val liveData_seed_comments: MutableLiveData<BaseResponse<Any>> by lazy {
        MutableLiveData<BaseResponse<Any>>()
    }
    val liveData_seed_sub_comments: MutableLiveData<BaseResponse<Any>> by lazy {
        MutableLiveData<BaseResponse<Any>>()
    }
    val liveData_comments_like: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }
    val liveData_sub_comments_like: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }

    val liveDataCommentReport: MutableLiveData<BaseResponse<ReportBean>> by lazy {
        MutableLiveData<BaseResponse<ReportBean>>()
    }


    fun requestComment() {
        commentsPage++
        viewModelScope.launch {
            repository.getCommentsFlow(comicsOrGames,id,commentsPage).collect {
                _comments.value = it
            }
        }
//        RetrofitUtil.service.comicsCommentsGet(
//            comics_games.toString(),
//            id.toString(),
//            page.toString(),
//            BaseHeaders("$comics_games/$id/comments?page=$page", "GET").getHeaderMapAndToken()
//        )
//            .doOnSubscribe(this@CommentsViewModel)
//            .subscribe(object : BaseObserver<CommentsBean>() {
//                override fun onSuccess(baseResponse: BaseResponse<CommentsBean>) {
//                    // 请求成功
//                    if (baseResponse.data.comments.page == 1 && baseResponse.data.topComments!=null) {
//                        val listDocs=ArrayList<CommentsBean.Comments.Doc>()
//                        for (index in baseResponse.data.topComments.size - 1 downTo  0) {
//                            //倒序遍历
//                            val commentsBean = CommentsBean.Comments.Doc(
//                                baseResponse.data.topComments[index]._id,
//                                baseResponse.data.topComments[index]._user,
//                                baseResponse.data.topComments[index].commentsCount,
//                                baseResponse.data.topComments[index].content,
//                                baseResponse.data.topComments[index].created_at,
//                                false,
//                                baseResponse.data.topComments[index]._id,
//                                baseResponse.data.topComments[index].isLiked,
//                                true,
//                                baseResponse.data.topComments[index].likesCount,
//                                baseResponse.data.topComments[index].totalComments,
//                                false
//                            )
//                            listDocs.add(commentsBean)
//                        }
//                        listDocs.addAll(baseResponse.data.comments.docs)
//                        baseResponse.data.comments.docs = listDocs
//                    }
//                    liveData_comments.postValue(baseResponse)
//                }
//
//                override fun onCodeError(baseResponse: BaseResponse<CommentsBean>) {
//                    page--
//                    liveData_comments.postValue(baseResponse)
//                }
//
//            })
    }
    fun requestSubComment() {
        subCommentsPage++

        viewModelScope.launch {
            repository.getSubCommentsFlow(commentId,commentsPage).collect {
                _subComments.value = it
            }
        }

//        RetrofitUtil.service.commentsChildrensGet(
//            commentId.toString(),
//            subCommentsPage.toString(),
//            BaseHeaders("comments/$commentId/childrens?page=$subCommentsPage", "GET").getHeaderMapAndToken()
//        )
//            .doOnSubscribe(this@CommentsViewModel)
//            .subscribe(object : BaseObserver<CommentsBean>() {
//                override fun onSuccess(baseResponse: BaseResponse<CommentsBean>) {
//                    if (subCommentsPage == 1 && baseResponse.data.comments.pages!=1) {
//                        //每次只加载5条数据，所以第一页多请求一遍 。还有只有一夜数据不重复加载
//                        requestSubComment()
//                    }
//                    liveData_sub_comments.postValue(baseResponse)
//                }
//
//                override fun onCodeError(baseResponse: BaseResponse<CommentsBean>) {
//                    subCommentsPage--
//                    liveData_sub_comments.postValue(baseResponse)
//                }
//
//            })
    }

    //post子评论回复https://picaapi.picacomic.com/comments/6388cc18d1001bff8cf4e925

    fun seedComments(comments: String) {
        RetrofitUtil.service.seedCommentsPost(
            comicsOrGames.toString(),
            id.toString(),
            RequestBody.create(
                "application/json; charset=UTF-8".toMediaTypeOrNull(),
                "{\"content\":\"$comments\"}"
            ),
            BaseHeaders("$comicsOrGames/$id/comments", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<Any>() {
                override fun onSuccess(baseResponse: BaseResponse<Any>) {
                    liveData_seed_comments.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<Any>) {
                    liveData_seed_comments.postValue(baseResponse)

                }
            })

    }

    fun seedSubComments(comments: String) {
        RetrofitUtil.service.seedSubCommentsPost(
            commentId.toString(),
            RequestBody.create(
                "application/json; charset=UTF-8".toMediaTypeOrNull(),
                "{\"content\":\"$comments\"}"
            ),
            BaseHeaders("comments/$commentId", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<Any>() {
                override fun onSuccess(baseResponse: BaseResponse<Any>) {
                    liveData_seed_sub_comments.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<Any>) {
                    liveData_seed_sub_comments.postValue(baseResponse)

                }
            })

    }
    fun commentsLike() {
        RetrofitUtil.service.commentsLikePost(
            likeCommentsId.toString(),
            BaseHeaders("comments/$likeCommentsId/like", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<ActionBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ActionBean>) {
                    liveData_comments_like.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<ActionBean>) {
                    liveData_comments_like.postValue(baseResponse)

                }
            })

    }

    fun subCommentsLike() {
        RetrofitUtil.service.commentsLikePost(
            likeSubCommentsId.toString(),
            BaseHeaders("comments/$likeSubCommentsId/like", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<ActionBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ActionBean>) {
                    liveData_sub_comments_like.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<ActionBean>) {
                    liveData_sub_comments_like.postValue(baseResponse)

                }
            })

    }

    fun commentsReport(commentId:String) {
        RetrofitUtil.service.commentsReportPost(
            commentId,
            BaseHeaders("comments/$commentId/report", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<ReportBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ReportBean>) {
                    liveDataCommentReport.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<ReportBean>) {
                    liveDataCommentReport.postValue(baseResponse)

                }
            })
    }
}