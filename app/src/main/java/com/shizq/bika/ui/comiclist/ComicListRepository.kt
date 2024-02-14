package com.shizq.bika.ui.comiclist

import com.shizq.bika.bean.ComicListBean
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType
import okhttp3.RequestBody
import java.net.URLEncoder

class ComicListRepository {
    suspend fun getComicListFlow(
        tag: String,
        sort: String,
        page: Int,
        value: String
    ): Flow<Result<ComicListBean>> = flow {
        var urlEnd = ""
        emit(Result.Loading)
        var response: BaseResponse<ComicListBean>? = null
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
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response =
                    if (value == "") {
                        RetrofitUtil.service.comicsListGet(page.toString(), sort, headers)
                    } else {
                        RetrofitUtil.service.comicsListGet(
                            page.toString(),
                            value.toString(),
                            sort,
                            headers
                        )
                    }
            }

            "latest" -> {
                urlEnd = "comics?page=$page&s=$sort"
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response = RetrofitUtil.service.comicsListGet(page.toString(), sort, headers)
            }

            "search" -> {
                urlEnd = "comics/advanced-search?page=$page"
                response =
                    RetrofitUtil.service.comicsSearchPOST(
                        page,
                        RequestBody.create(
                            MediaType.parse("application/json; charset=UTF-8"),
                            "{\"keyword\":\"$value\",\"sort\":\"$sort\"}"
                        ),
                        BaseHeaders(urlEnd.toString(), "POST").getHeaderMapAndToken()
                    )
            }

            "tags" -> {
                urlEnd = "comics?page=$page&t=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response = RetrofitUtil.service.tagsGet(page.toString(), value.toString(), headers)

            }

            "author" -> {
                urlEnd = "comics?page=$page&a=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response =
                    RetrofitUtil.service.authorGet(page.toString(), value.toString(), headers)

            }

            "knight" -> {
                urlEnd = "comics?page=$page&ca=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response =
                    RetrofitUtil.service.creatorGet(page.toString(), value.toString(), headers)

            }

            "translate" -> {
                urlEnd = "comics?page=$page&ct=" + URLEncoder.encode(value, "UTF-8")
                    .replace("\\+".toRegex(), "%20")
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response =
                    RetrofitUtil.service.translateGet(page.toString(), value.toString(), headers)

            }

            "favourite" -> {
                urlEnd = "users/favourite?s=$sort&page=$page"
                val headers = BaseHeaders(urlEnd.toString(), "GET").getHeaderMapAndToken()
                response = RetrofitUtil.service.myFavouriteGet(sort, page.toString(), headers)

            }
        }

        if (response?.code == 200) {
            val data = response.data
            if (data != null) {
                emit(Result.Success(data))
            } else {
                emit(Result.Error(response.code, "", "请求结果为空"))
            }
        } else {
            emit(Result.Error(response!!.code, response.error, response.message))
        }
    }.catch {
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)

    suspend fun getRandomFlow(): Flow<Result<ComicListBean2>> = flow {
        emit(Result.Loading)
        val headers = BaseHeaders("comics/random", "GET").getHeaderMapAndToken()
        val response = RetrofitUtil.service.randomGet(headers)
        if (response.code == 200) {
            val data = response.data
            if (data != null) {
                emit(Result.Success(data))
            } else {
                emit(Result.Error(response.code, "", "请求结果为空"))
            }
        } else {
            emit(Result.Error(response.code, response.error, response.message))
        }
    }.catch {
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)
}