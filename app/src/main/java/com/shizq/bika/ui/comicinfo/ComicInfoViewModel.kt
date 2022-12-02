package com.shizq.bika.ui.comicinfo

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.ChapterBean
import com.shizq.bika.bean.ComicInfoBean
import com.shizq.bika.bean.RecommendBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse

class ComicInfoViewModel(application: Application) : BaseViewModel(application) {
    var bookId: String? = null
    var title:String? =null
    var author:String? =null
    var totalViews:String? =null
    var chapterPage = 0
    var creatorId: String = ""
    var chapterTotal: Int = 1

    val liveData_info: MutableLiveData<BaseResponse<ComicInfoBean>> by lazy {
        MutableLiveData<BaseResponse<ComicInfoBean>>()
    }

    val liveData_chapter: MutableLiveData<BaseResponse<ChapterBean>> by lazy {
        MutableLiveData<BaseResponse<ChapterBean>>()
    }

    val liveData_recommend: MutableLiveData<BaseResponse<RecommendBean>> by lazy {
        MutableLiveData<BaseResponse<RecommendBean>>()
    }

    val liveData_like: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }

    val liveData_favourite: MutableLiveData<BaseResponse<ActionBean>> by lazy {
        MutableLiveData<BaseResponse<ActionBean>>()
    }


    //漫画信息
    fun getInfo() {
        RetrofitUtil.service.comicsInfoGet(
            bookId.toString(),
            BaseHeaders("comics/$bookId", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@ComicInfoViewModel)
            .subscribe(object : BaseObserver<ComicInfoBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ComicInfoBean>) {
                    creatorId = baseResponse.data.comic._creator._id
                    author = baseResponse.data.comic.author
                    // 请求成功
                    liveData_info.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ComicInfoBean>) {
                    liveData_info.postValue(baseResponse)
                }

            })
    }

    //章节
    fun getChapter() {
        //每次页数加1
        chapterPage++
        RetrofitUtil.service.comicsChapterGet(
            bookId.toString(),
            chapterPage.toString(),
            BaseHeaders("comics/$bookId/eps?page=$chapterPage", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@ComicInfoViewModel)
            .subscribe(object : BaseObserver<ChapterBean>() {

                override fun onSuccess(baseResponse: BaseResponse<ChapterBean>) {
                    chapterTotal = baseResponse.data.eps.total
                    // 请求成功
                    liveData_chapter.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ChapterBean>) {
                    chapterPage--
                    liveData_chapter.postValue(baseResponse)
                }

            })
    }

    //推荐
    fun getRecommend() {
        RetrofitUtil.service.recommendGet(
            bookId.toString(),
            BaseHeaders("comics/$bookId/recommendation", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@ComicInfoViewModel)
            .subscribe(object : BaseObserver<RecommendBean>() {
                override fun onSuccess(baseResponse: BaseResponse<RecommendBean>) {
                    liveData_recommend.postValue(baseResponse)
                }
                override fun onCodeError(baseResponse: BaseResponse<RecommendBean>) {}
            })
    }

    //爱心 喜欢
    fun getLike() {

        RetrofitUtil.service.comicsLikePost(
            bookId.toString(),
            BaseHeaders("comics/$bookId/like", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@ComicInfoViewModel)
            .subscribe(object : BaseObserver<ActionBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ActionBean>) {
                    liveData_like.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ActionBean>) {
                    liveData_like.postValue(baseResponse)
                }
            })
    }

    //收藏
    fun getFavourite() {

        RetrofitUtil.service.comicsFavouritePost(
            bookId.toString(),
            BaseHeaders("comics/$bookId/favourite", "POST").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@ComicInfoViewModel)
            .subscribe(object : BaseObserver<ActionBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ActionBean>) {
                    liveData_favourite.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ActionBean>) {
                    liveData_favourite.postValue(baseResponse)
                }

            })
    }
}