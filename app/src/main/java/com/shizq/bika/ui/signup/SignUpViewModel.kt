package com.shizq.bika.ui.signup

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.SignInBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class SignUpViewModel(application: Application) : BaseViewModel(application) {
    var name:String?=null
    var email:String?=null
    var password:String?=null
    var birthday:String?=null
    var gender:String?=null
    var question1:String?=null
    var question2:String?=null
    var question3:String?=null
    var answer1:String?=null
    var answer2:String?=null
    var answer3:String?=null

    val liveData_signup: MutableLiveData<BaseResponse<SignInBean>> by lazy {
        MutableLiveData<BaseResponse<SignInBean>>()
    }

    fun requestSignUp() {
//        Log.d("---json---",json.toString())
        val body = RequestBody.create(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            JsonObject().apply {
                addProperty("answer1", answer1)
                addProperty("answer2", answer2)
                addProperty("answer3", answer3)
                addProperty("birthday", birthday)
                addProperty("email", email)
                addProperty("gender", gender)
                addProperty("name", name)
                addProperty("password", password)
                addProperty("question1", question1)
                addProperty("question2", question2)
                addProperty("question3", question3)
            }.asJsonObject.toString()
        )
        val headers= BaseHeaders("auth/register","POST").getHeaders()

        RetrofitUtil.service.signUpPost(body,headers)
            .doOnSubscribe(this@SignUpViewModel)
            .subscribe(object : BaseObserver<SignInBean>() {
                override fun onSuccess(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signup.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<SignInBean>) {
                    liveData_signup.postValue(baseResponse)
                }
            })
    }
}