package com.shizq.bika.ui.main

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shizq.bika.MyApp
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.*
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import com.shizq.bika.utils.SPUtil
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.MediaType
import okhttp3.RequestBody

class MainViewModel(application: Application) : BaseViewModel(application) {
    var userId = "" //用来确认账号是否已经登录

    val liveData_profile: MutableLiveData<BaseResponse<ProfileBean>> by lazy {
        MutableLiveData<BaseResponse<ProfileBean>>()
    }

    val liveData_punch_in: MutableLiveData<BaseResponse<PunchInBean>> by lazy {
        MutableLiveData<BaseResponse<PunchInBean>>()
    }

    val liveData_signin: MutableLiveData<BaseResponse<SignInBean>> by lazy {
        MutableLiveData<BaseResponse<SignInBean>>()
    }

    val liveData: MutableLiveData<BaseResponse<CategoriesBean>> by lazy {
        MutableLiveData<BaseResponse<CategoriesBean>>()
    }

    val liveData_update: MutableLiveData<UpdateBean> by lazy {
        MutableLiveData<UpdateBean>()
    }

    val liveData_avatar: MutableLiveData<BaseResponse<Any>> by lazy {
        MutableLiveData<BaseResponse<Any>>()
    }

    fun getProfile() {
        RetrofitUtil.service.profileGet(BaseHeaders("users/profile", "GET").getHeaderMapAndToken())
            .doOnSubscribe(this@MainViewModel)
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

    fun punch_In() {
        val headers = BaseHeaders("users/punch-in", "POST").getHeaderMapAndToken()
        RetrofitUtil.service.punchInPOST(headers)
            .doOnSubscribe(this@MainViewModel)
            .subscribe(object : BaseObserver<PunchInBean>() {
                override fun onSuccess(baseResponse: BaseResponse<PunchInBean>) {
                    liveData_punch_in.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<PunchInBean>) {
                    liveData_punch_in.postValue(baseResponse)

                }
            })
    }

    fun getSignIn() {
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            JsonObject().apply {
                addProperty("email", SPUtil.get(MyApp.contextBase, "username", "") as String)
                addProperty("password", SPUtil.get(MyApp.contextBase, "password", "") as String)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/sign-in", "POST").getHeaders()
        RetrofitUtil.service.signInPost(body, headers)
            .doOnSubscribe(this@MainViewModel)
            .subscribe(object : BaseObserver<SignInBean>() {
                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signin.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signin.postValue(baseResponse)
                }
            })
    }

    fun getCategories() {
        val headers = BaseHeaders("categories", "GET").getHeaderMapAndToken()
        RetrofitUtil.service.categoriesGet(headers)
            .doOnSubscribe(this@MainViewModel)
            .subscribe(object : BaseObserver<CategoriesBean>() {
                override fun onSuccess(baseResponse: BaseResponse<CategoriesBean>) {
                    liveData.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<CategoriesBean>) {
                    liveData.postValue(baseResponse)
                }
            })
    }

    fun getUpdate() {
        RetrofitUtil.service_update.updateGet()
            .doOnSubscribe(this@MainViewModel)
            .subscribe(object : Observer<UpdateBean> {
                override fun onNext(t: UpdateBean) {
                    liveData_update.postValue(t)
                }

                override fun onError(e: Throwable) {}
                override fun onSubscribe(d: Disposable) {}
                override fun onComplete() {}

            })
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
            .doOnSubscribe(this@MainViewModel)
            .subscribe(object : BaseObserver<Any>() {
                override fun onSuccess(baseResponse: BaseResponse<Any>) {
                    liveData_avatar.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<Any>) {
                    liveData_avatar.postValue(baseResponse)
                }
            })
    }
}