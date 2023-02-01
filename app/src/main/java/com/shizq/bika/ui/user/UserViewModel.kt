package com.shizq.bika.ui.user

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ProfileBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import okhttp3.MediaType
import okhttp3.RequestBody

class UserViewModel(application: Application) : BaseViewModel(application) {
    val liveData_avatar: MutableLiveData<BaseResponse<Any>> by lazy {
        MutableLiveData<BaseResponse<Any>>()
    }

    val liveDataSlogan: MutableLiveData<BaseResponse<Any>> by lazy {
        MutableLiveData<BaseResponse<Any>>()
    }

    val liveData_profile: MutableLiveData<BaseResponse<ProfileBean>> by lazy {
        MutableLiveData<BaseResponse<ProfileBean>>()
    }

    fun putAvatar(base64Image: String) {
        val map = mutableMapOf(
            "avatar" to base64Image
        )
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            Gson().toJson(map)
        )
        val headers = BaseHeaders("users/avatar", "PUT").getHeaderMapAndToken()
        RetrofitUtil.service.avatarPUT(body, headers)
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<Any>() {
                override fun onSuccess(baseResponse: BaseResponse<Any>) {
                    liveData_avatar.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<Any>) {
                    liveData_avatar.postValue(baseResponse)
                }
            })
    }

    fun putProfile(slogan: String) {
        val map = mutableMapOf(
            "slogan" to slogan
        )
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            Gson().toJson(map)
        )
        val headers = BaseHeaders("users/profile", "PUT").getHeaderMapAndToken()
        RetrofitUtil.service.profilePUT(body, headers)
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<Any>() {
                override fun onSuccess(baseResponse: BaseResponse<Any>) {
                    liveDataSlogan.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<Any>) {
                    liveDataSlogan.postValue(baseResponse)
                }
            })
    }

    fun getProfile() {
        RetrofitUtil.service.profileGet(BaseHeaders("users/profile", "GET").getHeaderMapAndToken())
            .doOnSubscribe(this)
            .subscribe(object : BaseObserver<ProfileBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ProfileBean>) {
                    // 请求成功
                    liveData_profile.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ProfileBean>) {
                    liveData_profile.postValue(baseResponse)
                }

            })
    }
}