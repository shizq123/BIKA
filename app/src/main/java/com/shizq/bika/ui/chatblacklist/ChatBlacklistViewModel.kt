package com.shizq.bika.ui.chatblacklist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatBlackListBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.HttpException

class  ChatBlacklistViewModel(application: Application) : BaseViewModel(application) {
    var offset = -1

    val liveDataBlackList: MutableLiveData<ChatBlackListBean> by lazy {
        MutableLiveData<ChatBlackListBean>()
    }

    fun getChatBlackList() {
        offset++
        RetrofitUtil.service_live.ChatBlackListGet(offset,
            BaseHeaders().getChatHeaderMapAndToken()
        )
            .doOnSubscribe(this)
            .subscribe(object : Observer<ChatBlackListBean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    var t: ChatBlackListBean? = null
                    if (e is HttpException) {   //  处理服务器返回的非成功异常
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<ChatBlackListBean>() {}.type
                            t = Gson().fromJson(responseBody.string(), type)
                            liveDataBlackList.postValue(t)
                        } else {
                            liveDataBlackList.postValue(t)
                        }
                    }
                }

                override fun onComplete() {

                }

                override fun onNext(t: ChatBlackListBean) {
                    liveDataBlackList.postValue(t)
                }

            })

    }
}