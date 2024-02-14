package com.shizq.bika.ui.collections

import com.shizq.bika.bean.CollectionsBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CollectionsRepository {
    suspend fun getDataFlow(): Flow<Result<CollectionsBean>> = flow {
        emit(Result.Loading)
        val response  = RetrofitUtil.service.collectionsGet(BaseHeaders("collections","GET").getHeaderMapAndToken())
        if (response.code==200) {
            val data = response.data
            if (data != null) {
                emit(Result.Success(data))
            } else {
                emit(Result.Error(response.code,"","请求结果为空"))
            }
        } else {
            emit(Result.Error(response.code,response.error,response.message))
        }
    }.catch{
        emit(Result.Error(-1,"","请求结果异常"))
    }.flowOn(Dispatchers.IO)
}