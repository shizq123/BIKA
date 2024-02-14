package com.shizq.bika.ui.comicinfo

import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.EpisodeBean
import com.shizq.bika.bean.ComicInfoBean
import com.shizq.bika.bean.RecommendBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ComicInfoRepository {
    suspend fun getInfoFlow(bookId: String): Flow<Result<ComicInfoBean>> = flow {
        emit(Result.Loading)
        val response = RetrofitUtil.service.comicsInfoGet(
            bookId,
            BaseHeaders("comics/$bookId", "GET").getHeaderMapAndToken()
        )
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
        //需要异常信息处理 （例如code=400 error=1014 审核中）
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)

    suspend fun getEpisodeFlow(bookId: String, page: String): Flow<Result<EpisodeBean>> = flow {
            emit(Result.Loading)
            val headers = BaseHeaders("comics/$bookId/eps?page=$page", "GET").getHeaderMapAndToken()
            val response = RetrofitUtil.service.comicsEpisodeGet(bookId, page, headers)
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
            //需要异常信息处理 （例如code=400 error=1014 审核中）
            emit(Result.Error(-1, "", "请求结果异常"))
        }.flowOn(Dispatchers.IO)

    suspend fun getRecommendFlow(bookId: String): Flow<Result<RecommendBean>> = flow {
            emit(Result.Loading)
            val headers = BaseHeaders("comics/$bookId/recommendation", "GET").getHeaderMapAndToken()
            val response = RetrofitUtil.service.recommendGet(bookId, headers)
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
            //需要异常信息处理 （例如code=400 error=1014 审核中）
            emit(Result.Error(-1, "", "请求结果异常"))
        }.flowOn(Dispatchers.IO)

    suspend fun getLikeFlow(bookId: String): Flow<Result<ActionBean>> = flow {
        emit(Result.Loading)
        val headers = BaseHeaders("comics/$bookId/like", "POST").getHeaderMapAndToken()
        val response = RetrofitUtil.service.comicsLikePost(bookId, headers)
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
        //需要异常信息处理 （例如code=400 error=1014 审核中）
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)

    suspend fun getFavouriteFlow(bookId: String): Flow<Result<ActionBean>> = flow {
        emit(Result.Loading)
        val headers = BaseHeaders("comics/$bookId/favourite", "POST").getHeaderMapAndToken()
        val response = RetrofitUtil.service.comicsFavouritePost(bookId, headers)
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
        //需要异常信息处理 （例如code=400 error=1014 审核中）
        emit(Result.Error(-1, "", "请求结果异常"))
    }.flowOn(Dispatchers.IO)
}