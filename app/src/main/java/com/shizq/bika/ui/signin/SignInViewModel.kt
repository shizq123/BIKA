package com.shizq.bika.ui.signin

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.shizq.bika.bean.SignInBean
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class SignInViewModel(application: Application) {
    var forgot_answer: String? = null
    var forgot_email: String? = null
    var forgot_questionNo: String? = null

    val liveData_forgot = MutableLiveData<BaseResponse<SignInBean>>()

    val liveData_password = MutableLiveData<BaseResponse<SignInBean>>()

    fun getForgot() {
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply { addProperty("email", forgot_email) }.asJsonObject.toString()
        )
        val headers = BaseHeaders("auth/forgot-password", "POST").getHeaders()

//        RetrofitUtil.service.forgotPasswordPost(body, headers)
//            .doOnSubscribe(this@SignInViewModel)
//            .subscribe(object : BaseObserver<SignInBean>() {
//                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
//                    liveData_forgot.postValue(baseResponse)
//                }
//
//                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
//                    liveData_forgot.postValue(baseResponse)
//                }
//            })
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

//        RetrofitUtil.service.resetPasswordPost(body, headers)
//            .doOnSubscribe(this@SignInViewModel)
//            .subscribe(object : BaseObserver<SignInBean>() {
//
//                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
//                    liveData_password.postValue(baseResponse)
//                }
//
//                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
//                    liveData_password.postValue(baseResponse)
//
//                }
//            })
    }
}