package com.shizq.bika.ui.search

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.KeywordsBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse

class SearchViewModel(application: Application) : BaseViewModel(application) {
    val liveData_key: MutableLiveData<BaseResponse<KeywordsBean>> by lazy {
        MutableLiveData<BaseResponse<KeywordsBean>>()
    }

    fun getKey() {
        val headers= BaseHeaders("keywords","GET").getHeaderMapAndToken()

        RetrofitUtil.service.keywordsGet(headers)
            .doOnSubscribe(this@SearchViewModel)
            .subscribe(object : BaseObserver<KeywordsBean>() {

                override fun onSuccess(baseResponse: BaseResponse<KeywordsBean>) {
                    liveData_key.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<KeywordsBean>) {
                    liveData_key.postValue(baseResponse)
                }
            })
    }

}