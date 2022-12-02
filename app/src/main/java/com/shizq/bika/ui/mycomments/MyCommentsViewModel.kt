package com.shizq.bika.ui.mycomments

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.CommentsBean
import com.shizq.bika.bean.MyCommentsBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse

class MyCommentsViewModel (application: Application) : BaseViewModel(application) {
    var page = 0
    var commentsId: String? = null
    var subPage = 0

    var likePosition = -1
    var likeSubPosition = -1
    var likeCommentsId: String? = null
    var likeSubCommentsId: String? = null

    val liveData_comments: MutableLiveData<BaseResponse<MyCommentsBean>> by lazy {
        MutableLiveData<BaseResponse<MyCommentsBean>>()
    }

    val liveData_sub_comments: MutableLiveData<BaseResponse<CommentsBean>> by lazy {
        MutableLiveData<BaseResponse<CommentsBean>>()
    }

    val liveData_comments_like: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }
    val liveData_sub_comments_like: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }

    fun requestComment() {
        page++
        RetrofitUtil.service.myCommentsGet(
            page.toString(),
            BaseHeaders("users/my-comments?page=$page", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<MyCommentsBean>() {
                override fun onSuccess(baseResponse: BaseResponse<MyCommentsBean>) {
                    liveData_comments.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<MyCommentsBean>) {
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
            .doOnSubscribe(this)
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

    fun commentsLike() {
        RetrofitUtil.service.commentsLikePost(
            likeCommentsId.toString(),
            BaseHeaders("comments/$likeCommentsId/like", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this)
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
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<ActionBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ActionBean>) {
                    liveData_sub_comments_like.postValue(baseResponse)

                }

                override fun onCodeError(baseResponse: BaseResponse<ActionBean>) {
                    liveData_sub_comments_like.postValue(baseResponse)

                }
            })

    }

}