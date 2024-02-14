package com.shizq.bika.ui.apps

import com.shizq.bika.bean.ChatRoomListOldBean
import com.shizq.bika.bean.PicaAppsBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppsRepository {

    suspend fun getRoomListFlow(): Flow<Result<ChatRoomListOldBean>> = flow {
        emit(Result.Loading)
        val response  = RetrofitUtil.service.oldChatRoomListGet(BaseHeaders("chat", "GET").getHeaderMapAndToken())
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

    suspend fun getAppsFlow(): Flow<Result<PicaAppsBean>> = flow {
        emit(Result.Loading)
        val response  = RetrofitUtil.service.picaAppsGet(BaseHeaders("pica-apps", "GET").getHeaderMapAndToken())
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