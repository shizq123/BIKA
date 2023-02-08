package com.shizq.bika.ui.chatroom

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shizq.bika.MyApp
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.bean.ChatSignInBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.utils.SPUtil
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.HttpException

class ChatRoomListViewModel(application: Application) : BaseViewModel(application) {
    val liveDataSignIn: MutableLiveData<ChatSignInBean> by lazy {
        MutableLiveData<ChatSignInBean>()
    }

    val liveDataRoomList: MutableLiveData<ChatRoomListBean> by lazy {
        MutableLiveData<ChatRoomListBean>()
    }

    fun chatSignIn() {
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            JsonObject().apply {
                addProperty("email", SPUtil.get(MyApp.contextBase, "username", "") as String)
                addProperty("password", SPUtil.get(MyApp.contextBase, "password", "") as String)
            }.asJsonObject.toString()
        )
        RetrofitUtil.service_live.ChatSignInPost(
            body,
            BaseHeaders().getChatHeaders()
        )
            .doOnSubscribe(this)
            .subscribe(object : Observer<ChatSignInBean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    var t: ChatSignInBean? = null
                    if (e is HttpException) {   //  处理服务器返回的非成功异常
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<ChatSignInBean>() {}.type
                            t = Gson().fromJson(responseBody.string(), type)
                            liveDataSignIn.postValue(t)
                        } else {
                            liveDataSignIn.postValue(t)
                        }
                    }
                }

                override fun onComplete() {

                }

                override fun onNext(t: ChatSignInBean) {
                    liveDataSignIn.postValue(t)
                }

            })
    }

    fun chatRoomList() {
        RetrofitUtil.service_live.ChatRoomListGet(
            BaseHeaders().getChatHeaderMapAndToken()
        )
            .doOnSubscribe(this)
            .subscribe(object : Observer<ChatRoomListBean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    var t: ChatRoomListBean? = null
                    if (e is HttpException) {   //  处理服务器返回的非成功异常
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<ChatRoomListBean>() {}.type
                            t = Gson().fromJson(responseBody.string(), type)
                            liveDataRoomList.postValue(t)
                        } else {
                            liveDataRoomList.postValue(t)
                        }
                    }
                }

                override fun onComplete() {

                }

                override fun onNext(t: ChatRoomListBean) {
                    liveDataRoomList.postValue(t)
                }

            })

    }
}