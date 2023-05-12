package com.shizq.bika.ui.chatroom.current.roomlist

import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ChatRoomListRepository {
    suspend fun getChatRoomListFlow(): Flow<Observable<ChatRoomListBean>> = flow {
        val response  = RetrofitUtil.service.chatRoomListGet(BaseHeaders().getChatHeaderMapAndToken())
        emit(response )
    }.flowOn(Dispatchers.IO)
}