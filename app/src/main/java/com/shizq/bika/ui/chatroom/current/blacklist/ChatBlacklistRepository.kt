package com.shizq.bika.ui.chatroom.current.blacklist

import com.shizq.bika.bean.ChatRoomBlackListBean
import com.shizq.bika.bean.ChatRoomBlackListDeleteBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ChatBlacklistRepository {suspend fun getBlackListFlow(pageNum: Int): Flow<Result<ChatRoomBlackListBean>> = flow {
    emit(Result.Loading)
    val response = RetrofitUtil.service_live.chatBlackListGet(pageNum, BaseHeaders().getChatHeaderMapAndToken())

    if (response.limit != 0) {
        emit(Result.Success(response))
    } else {
        emit(Result.Error(response.statusCode, response.error, response.message))
    }

}.catch {
    // TODO 需要补充异常处理（code error message）
    emit(Result.Error(-1, "请求结果异常", it.message.toString()))
}.flowOn(Dispatchers.IO)

    suspend fun deleteBlackListFlow(id: String): Flow<Result<ChatRoomBlackListDeleteBean>> = flow {
        emit(Result.Loading)
        val response = RetrofitUtil.service_live.chatBlackListDelete(id, BaseHeaders().getChatHeaderMapAndToken())

        if (response.id != null&&response.id != "") {
            emit(Result.Success(response))
        } else {
            emit(Result.Error(response.statusCode, response.error, response.message))
        }

    }.catch {
        // TODO 需要补充异常处理（code error message）
        emit(Result.Error(-1, "请求结果异常", it.message.toString()))
    }.flowOn(Dispatchers.IO)

}