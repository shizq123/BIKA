package com.shizq.bika.ui.chatroom.current.blacklist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatRoomBlackListBean
import com.shizq.bika.bean.ChatRoomBlackListDeleteBean
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class  ChatBlacklistViewModel(application: Application) : BaseViewModel(application) {
    var offset = -1
    private val repository = ChatBlacklistRepository()
    private val _blackListFlow = MutableStateFlow<Result<ChatRoomBlackListBean>?>(null)
    val blackListFlow: StateFlow<Result<ChatRoomBlackListBean>?> = _blackListFlow
    private val _blackListDeleteFlow = MutableStateFlow<Result<ChatRoomBlackListDeleteBean>?>(null)
    val blackListDeleteFlow: StateFlow<Result<ChatRoomBlackListDeleteBean>?> = _blackListDeleteFlow

    val liveDataBlackList: MutableLiveData<ChatRoomBlackListBean> by lazy {
        MutableLiveData<ChatRoomBlackListBean>()
    }
    val liveDataBlackListDelete: MutableLiveData<ChatRoomBlackListDeleteBean> by lazy {
        MutableLiveData<ChatRoomBlackListDeleteBean>()
    }

    fun getChatBlackList() {
        offset++
//        RetrofitUtil.service_live.chatBlackListGet(offset,
//            BaseHeaders().getChatHeaderMapAndToken()
//        )
//            .doOnSubscribe(this)
//            .subscribe(object : Observer<ChatRoomBlackListBean> {
//                override fun onSubscribe(d: Disposable) {
//
//                }
//
//                override fun onError(e: Throwable) {
//                    var t: ChatRoomBlackListBean? = null
//                    if (e is HttpException) {   //  处理服务器返回的非成功异常
//                        val responseBody = e.response()!!.errorBody()
//                        if (responseBody != null) {
//                            val type = object : TypeToken<ChatRoomBlackListBean>() {}.type
//                            t = Gson().fromJson(responseBody.string(), type)
//                            liveDataBlackList.postValue(t)
//                        } else {
//                            liveDataBlackList.postValue(t)
//                        }
//                    }
//                }
//
//                override fun onComplete() {
//
//                }
//
//                override fun onNext(t: ChatRoomBlackListBean) {
//                    liveDataBlackList.postValue(t)
//                }
//
//            })

        viewModelScope.launch {
            repository.getBlackListFlow(offset).collect {
                _blackListFlow.value = it
            }
        }
    }

    fun deleteChatBlackList(id:String) {
//        RetrofitUtil.service_live.chatBlackListDelete(id,
//            BaseHeaders().getChatHeaderMapAndToken()
//        )
//            .doOnSubscribe(this)
//            .subscribe(object : Observer<ChatRoomBlackListDeleteBean> {
//                override fun onSubscribe(d: Disposable) {
//
//                }
//
//                override fun onError(e: Throwable) {
//                    var t: ChatRoomBlackListDeleteBean? = null
//                    if (e is HttpException) {   //  处理服务器返回的非成功异常
//                        val responseBody = e.response()!!.errorBody()
//                        if (responseBody != null) {
//                            val type = object : TypeToken<ChatRoomBlackListDeleteBean>() {}.type
//                            t = Gson().fromJson(responseBody.string(), type)
//                            liveDataBlackListDelete.postValue(t)
//                        } else {
//                            liveDataBlackListDelete.postValue(t)
//                        }
//                    }
//                }
//
//                override fun onComplete() {
//
//                }
//
//                override fun onNext(t: ChatRoomBlackListDeleteBean) {
//                    liveDataBlackListDelete.postValue(t)
//                }
//
//            })
        viewModelScope.launch {
            repository.deleteBlackListFlow(id).collect {
                _blackListDeleteFlow.value = it
            }
        }

    }
}