package com.shizq.bika.ui.comment

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import okhttp3.MediaType
import okhttp3.RequestBody

class CommentsViewModel(application: Application) : BaseViewModel(application) {
    var data: CommentsBean.Comments.Doc?=null
    var id: String? = null
    var comics_games: String? = null
    var page = 0
    var commentsId: String? = null
    var subPage = 0
    var likePosition = -1
    var likeSubPosition = -1
    var likeCommentsId: String? = null
    var likeSubCommentsId: String? = null


    val liveData_comments: MutableLiveData<BaseResponse<CommentsBean>> by lazy {
        MutableLiveData<BaseResponse<CommentsBean>>()
    }

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


    fun requestComment() {
        page++
        RetrofitUtil.service.comicsCommentsGet(
            comics_games.toString(),
            id.toString(),
            page.toString(),
            BaseHeaders("$comics_games/$id/comments?page=$page", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<CommentsBean>() {
                override fun onSuccess(baseResponse: BaseResponse<CommentsBean>) {
                    // 请求成功
                    if (baseResponse.data.comments.page == 1 && baseResponse.data.topComments!=null) {
                        val listDocs=ArrayList<CommentsBean.Comments.Doc>()
                        for (index in baseResponse.data.topComments.size - 1 downTo  0) {
                            //倒序遍历
                            val commentsBean = CommentsBean.Comments.Doc(
                                baseResponse.data.topComments[index]._id,
                                baseResponse.data.topComments[index]._user,
                                baseResponse.data.topComments[index].commentsCount,
                                baseResponse.data.topComments[index].content,
                                baseResponse.data.topComments[index].created_at,
                                false,
                                baseResponse.data.topComments[index]._id,
                                baseResponse.data.topComments[index].isLiked,
                                true,
                                baseResponse.data.topComments[index].likesCount,
                                baseResponse.data.topComments[index].totalComments,
                                false
                            )
                            listDocs.add(commentsBean)
                        }
                        listDocs.addAll(baseResponse.data.comments.docs)
                        baseResponse.data.comments.docs = listDocs
                    }
                    liveData_comments.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<CommentsBean>) {
                    page--
                    liveData_comments.postValue(baseResponse)
                }

            })
    }
    fun requestSubComment() {
        subPage++
        RetrofitUtil.service.commentsChildrensGet(
            commentsId.toString(),
            subPage.toString(),
            BaseHeaders("comments/$commentsId/childrens?page=$subPage", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@CommentsViewModel)
            .subscribe(object : BaseObserver<CommentsBean>() {
                override fun onSuccess(baseResponse: BaseResponse<CommentsBean>) {
                    if (subPage == 1 && baseResponse.data.comments.pages!=1) {
                        //每次只加载5条数据，所以第一页多请求一遍 。还有只有一夜数据不重复加载
                        requestSubComment()
                    }
                    liveData_sub_comments.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<CommentsBean>) {
                    subPage--
                    liveData_sub_comments.postValue(baseResponse)
                }

            })
    }

    //post子评论回复https://picaapi.picacomic.com/comments/6388cc18d1001bff8cf4e925

    fun seedComments(comments: String) {
        RetrofitUtil.service.seedCommentsPost(
            comics_games.toString(),
            id.toString(),
            RequestBody.create(
                MediaType.parse("application/json; charset=UTF-8"),
                "{\"content\":\"$comments\"}"
            ),
            BaseHeaders("$comics_games/$id/comments", "POST").getHeaderMapAndToken()
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
            commentsId.toString(),
            RequestBody.create(
                MediaType.parse("application/json; charset=UTF-8"),
                "{\"content\":\"$comments\"}"
            ),
            BaseHeaders("comments/$commentsId", "POST").getHeaderMapAndToken()
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

    fun commentsReport() {


    }
}