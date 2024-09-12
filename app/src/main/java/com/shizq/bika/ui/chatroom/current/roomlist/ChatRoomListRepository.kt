package com.shizq.bika.ui.chatroom.current.roomlist

import com.google.gson.JsonObject
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.bean.ChatRoomSignInBean
import com.shizq.bika.network.Result
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.utils.SPUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class ChatRoomListRepository {
    suspend fun getSignInFlow(): Flow<Result<ChatRoomSignInBean>> = flow {
        emit(Result.Loading)
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply {
                addProperty("email", SPUtil.get("username", "") as String)
                addProperty("password", SPUtil.get("password", "") as String)
            }.asJsonObject.toString()
        )
        val response = RetrofitUtil.service_live.chatSignInPost(
            body,
            BaseHeaders().getChatHeaders()
        )
        if (response.token != "") {
            emit(Result.Success(response))
        } else {
            emit(Result.Error(response.statusCode, response.error, response.message))
        }

    }.catch {
        // TODO 需要补充异常处理（code error message）
        emit(Result.Error(-1, "请求结果异常", it.message.toString()))
    }.flowOn(Dispatchers.IO)

    suspend fun getRoomListFlow(): Flow<Result<ChatRoomListBean>> = flow {
        emit(Result.Loading)
        val response = RetrofitUtil.service_live.chatRoomListGet(BaseHeaders().getChatHeaderMapAndToken())

        if (response.rooms != null) {
            emit(Result.Success(response))
        } else {
            emit(Result.Error(response.statusCode, response.error, response.message))
        }

    }.catch {
        // TODO 需要补充异常处理（code error message）
        emit(Result.Error(-1, "请求结果异常", it.message.toString()))
    }.flowOn(Dispatchers.IO)

}