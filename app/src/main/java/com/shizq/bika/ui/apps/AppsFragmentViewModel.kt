package com.shizq.bika.ui.apps

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatRoomListOldBean
import com.shizq.bika.bean.PicaAppsBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse

class AppsFragmentViewModel(application: Application) : BaseViewModel(application) {
    val liveData_chat: MutableLiveData<BaseResponse<ChatRoomListOldBean>> by lazy {
        MutableLiveData<BaseResponse<ChatRoomListOldBean>>()
    }

    val liveData_apps: MutableLiveData<BaseResponse<PicaAppsBean>> by lazy {
        MutableLiveData<BaseResponse<PicaAppsBean>>()
    }

    fun getChatList() {
        RetrofitUtil.service.chatListGet(
            BaseHeaders("chat", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<ChatRoomListOldBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ChatRoomListOldBean>) {
                    liveData_chat.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ChatRoomListOldBean>) {
                    liveData_chat.postValue(baseResponse)
                }

            })
    }

    fun getPicaApps() {
        RetrofitUtil.service.picaAppsGet(
            BaseHeaders("pica-apps", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<PicaAppsBean>() {
                override fun onSuccess(baseResponse: BaseResponse<PicaAppsBean>) {
                    liveData_apps.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<PicaAppsBean>) {
                    liveData_apps.postValue(baseResponse)
                }

            })
    }
}