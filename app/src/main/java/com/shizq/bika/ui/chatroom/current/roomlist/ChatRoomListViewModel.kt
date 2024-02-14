package com.shizq.bika.ui.chatroom.current.roomlist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.bean.ChatRoomSignInBean
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatRoomListViewModel(application: Application) : BaseViewModel(application) {
    val liveDataSignIn: MutableLiveData<ChatRoomSignInBean> by lazy {
        MutableLiveData<ChatRoomSignInBean>()
    }

    private val repository = ChatRoomListRepository()

    private val _signInFlow = MutableStateFlow<Result<ChatRoomSignInBean>?>(null)
    val signInFlow: StateFlow<Result<ChatRoomSignInBean>?> = _signInFlow

    private val _roomListFlow = MutableStateFlow<Result<ChatRoomListBean>?>(null)
    val roomListFlow: StateFlow<Result<ChatRoomListBean>?> = _roomListFlow

    fun chatSignIn() {
//        val body = RequestBody.create(
//            MediaType.parse("application/json; charset=UTF-8"),
//            JsonObject().apply {
//                addProperty("email", SPUtil.get(MyApp.contextBase, "username", "") as String)
//                addProperty("password", SPUtil.get(MyApp.contextBase, "password", "") as String)
//            }.asJsonObject.toString()
//        )
//        RetrofitUtil.service_live.chatSignInPost(
//            body,
//            BaseHeaders().getChatHeaders()
//        )
//            .doOnSubscribe(this)
//            .subscribe(object : Observer<ChatRoomSignInBean> {
//                override fun onSubscribe(d: Disposable) {
//
//                }
//
//                override fun onError(e: Throwable) {
//                    var t: ChatRoomSignInBean? = null
//                    if (e is HttpException) {   //  处理服务器返回的非成功异常
//                        val responseBody = e.response()!!.errorBody()
//                        if (responseBody != null) {
//                            val type = object : TypeToken<ChatRoomSignInBean>() {}.type
//                            t = Gson().fromJson(responseBody.string(), type)
//                            liveDataSignIn.postValue(t)
//                        } else {
//                            liveDataSignIn.postValue(t)
//                        }
//                    }
//                }
//
//                override fun onComplete() {
//
//                }
//
//                override fun onNext(t: ChatRoomSignInBean) {
//                    liveDataSignIn.postValue(t)
//                }
//
//            })
        viewModelScope.launch {
            repository.getSignInFlow().collect {
                _signInFlow.value = it
            }
        }
    }

    fun chatRoomList() {
//        RetrofitUtil.service_live.chatRoomListGet(
//            BaseHeaders().getChatHeaderMapAndToken()
//        )
//            .doOnSubscribe(this)
//            .subscribe(object : Observer<ChatRoomListBean> {
//                override fun onSubscribe(d: Disposable) {
//
//                }
//
//                override fun onError(e: Throwable)   {
//                    var t: ChatRoomListBean? = null
//                    if (e is HttpException) {   //  处理服务器返回的非成功异常
//                        val responseBody = e.response()!!.errorBody()
//                        if (responseBody != null) {
//                            val type = object : TypeToken<ChatRoomListBean>() {}.type
//                            t = Gson().fromJson(responseBody.string(), type)
//                            liveDataRoomList.postValue(t)
//                        } else {
//                            liveDataRoomList.postValue(t)
//                        }
//                    }
//                }
//
//                override fun onComplete() {
//
//                }
//
//                override fun onNext(t: ChatRoomListBean) {
//                    liveDataRoomList.postValue(t)
//                }
//
//            })

        viewModelScope.launch {
            repository.getRoomListFlow().collect {
                _roomListFlow.value = it
            }
        }

    }
}