package com.shizq.bika.ui.signin

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ProfileBean
import com.shizq.bika.bean.SignInBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class SignInViewModel(application: Application) : BaseViewModel(application) {
    var forgot_answer: String? = null
    var forgot_email: String? = null
    var forgot_questionNo: String? = null
    var email: String? = null
    var password: String? = null

    val liveData_signin: MutableLiveData<BaseResponse<SignInBean>> by lazy {
        MutableLiveData<BaseResponse<SignInBean>>()
    }

    val liveData_forgot: MutableLiveData<BaseResponse<SignInBean>> by lazy {
        MutableLiveData<BaseResponse<SignInBean>>()
    }

    val liveData_password: MutableLiveData<BaseResponse<SignInBean>> by lazy {
        MutableLiveData<BaseResponse<SignInBean>>()
    }


    fun getSignIn() {
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/sign-in", "POST").getHeaders()

        RetrofitUtil.service.signInPost(body, headers)
            .doOnSubscribe(this@SignInViewModel)
            .subscribe(object : BaseObserver<SignInBean>() {
                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signin.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signin.postValue(baseResponse)
                }
            })
    }

    fun getForgot() {
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply { addProperty("email", forgot_email) }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/forgot-password", "POST").getHeaders()

        RetrofitUtil.service.forgotPasswordPost(body, headers)
            .doOnSubscribe(this@SignInViewModel)
            .subscribe(object : BaseObserver<SignInBean>() {
                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
                    liveData_forgot.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
                    liveData_forgot.postValue(baseResponse)
                }
            })
    }

    fun resetPassword() {
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply {
                addProperty("answer", forgot_answer)
                addProperty("email", forgot_email)
                addProperty("questionNo", forgot_questionNo)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/reset-password", "POST").getHeaders()

        RetrofitUtil.service.resetPasswordPost(body, headers)
            .doOnSubscribe(this@SignInViewModel)
            .subscribe(object : BaseObserver<SignInBean>() {

                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
                    liveData_password.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
                    liveData_password.postValue(baseResponse)

                }
            })
    }
}