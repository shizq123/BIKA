package com.shizq.bika.ui.comiclist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ComicListBean
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import io.reactivex.rxjava3.core.Observable
import okhttp3.MediaType
import okhttp3.RequestBody
import java.net.URLEncoder

class ComicListViewModel(application: Application) : BaseViewModel(application) {
    var urlEnd: String? = null
    var tag: String? = null
    var startpage = 0//起始页数，用于跳转页数后判断当前页数
    var page = 0//当前页数
    var pages = 1//总页数
    var limit = 20//每页显示多少
    var sort: String = "dd"
    var value: String? = null

    var baseObservable: Observable<BaseResponse<ComicListBean>>? = null

    val liveData: MutableLiveData<BaseResponse<ComicListBean>> by lazy {
        MutableLiveData<BaseResponse<ComicListBean>>()
    }
    val liveData2: MutableLiveData<BaseResponse<ComicListBean2>> by lazy {
        MutableLiveData<BaseResponse<ComicListBean2>>()
    }

    fun getComicList() {

        page++//每次请求加1
        //不同的tag用不同的url
        when (tag) {
            "categories" -> {
                urlEnd =
                    if (value == "") {
                        "comics?page=$page&s=$sort"
                    } else {
                        "comics?page=$page&c=${
                            URLEncoder.encode(value, "UTF-8").replace("\\+".toRegex(), "%20")
                        }&s=$sort"
                    }
            }
            "latest" -> {
                urlEnd = "comics?page=$page&s=$sort"
            }

            "search" -> {
                urlEnd = "comics/advanced-search?page=$page"
            }
            "tags" -> {
                urlEnd = "comics?page=$page&t=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
            }
            "author" -> {
                urlEnd = "comics?page=$page&a=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
            }
            "knight" -> {
                urlEnd = "comics?page=$page&ca=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
            }
            "translate" -> {
                urlEnd = "comics?page=$page&ct=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
            }
            "favourite" -> {
                urlEnd = "users/favourite?s=$sort&page=$page"
            }
        }

        //不同的tag用不同的Observable
        val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
        when (tag) {

            "categories" -> {

                if (value == "") {
                    baseObservable =
                        RetrofitUtil.service.comicsListGet(page.toString(), sort, headers)
                } else {
                    baseObservable = RetrofitUtil.service.comicsListGet(
                        page.toString(),
                        value.toString(),
                        sort,
                        headers
                    )

                }
            }
            "latest" -> {
                baseObservable = RetrofitUtil.service.comicsListGet(page.toString(), sort, headers)
            }

            "search" -> {
                baseObservable = RetrofitUtil.service.comicsSearchPOST(
                    page,
                    RequestBody.create(
                        MediaType.parse("application/json; charset=UTF-8"),
                        "{\"keyword\":\"$value\",\"sort\":\"$sort\"}"
                    ),
                    BaseHeaders(urlEnd.toString(), "POST").getHeaderMapAndToken()
                )
            }

            "tags" -> {
                baseObservable = RetrofitUtil.service.tagsGet(page.toString(), value.toString(), headers)
            }
            "author" -> {
                baseObservable = RetrofitUtil.service.authorGet(page.toString(), value.toString(), headers)
            }
            "knight" -> {
                baseObservable = RetrofitUtil.service.creatorGet(page.toString(), value.toString(), headers)
            }
            "translate" -> {
                baseObservable = RetrofitUtil.service.translateGet(page.toString(), value.toString(), headers)
            }
            "favourite" -> {
                baseObservable = RetrofitUtil.service.myFavouriteGet(sort, page.toString(), headers)
            }
        }

        //网络请求
        baseObservable
            ?.doOnSubscribe(this@ComicListViewModel)
            ?.subscribe(object : BaseObserver<ComicListBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ComicListBean>) {
                    // 请求成功
                    liveData.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ComicListBean>) {
                    page--
                    liveData.postValue(baseResponse)
                }

            })


    }

    fun getRandom() {
        RetrofitUtil.service.randomGet(BaseHeaders("comics/random", "GET").getHeaderMapAndToken())
            .doOnSubscribe(this@ComicListViewModel)
            .subscribe(object : BaseObserver<ComicListBean2>() {
                override fun onSuccess(baseResponse: BaseResponse<ComicListBean2>) {
                    // 请求成功
                    liveData2.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ComicListBean2>) {
                    // 请求成功
                    liveData2.postValue(baseResponse)
                }

            })
    }
}