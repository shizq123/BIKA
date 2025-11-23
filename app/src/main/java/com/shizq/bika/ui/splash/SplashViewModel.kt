package com.shizq.bika.ui.splash

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.InitBean
import com.shizq.bika.bean.UpdateBean
import com.shizq.bika.network.RetrofitUtil
import io.reactivex.rxjava3.observers.DefaultObserver

class SplashViewModel(application: Application) : BaseViewModel(application) {
    val liveData_latest_version: MutableLiveData<UpdateBean> by lazy {
        MutableLiveData<UpdateBean>()
    }

    val liveData_init: MutableLiveData<InitBean> by lazy {
        MutableLiveData<InitBean>()
    }
    val liveData_init_error: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun getInit() {
        RetrofitUtil.service_init.initGet()
            .doOnSubscribe(this@SplashViewModel)
            .subscribe(object : DefaultObserver<InitBean>() {

                override fun onNext(bean: InitBean) {
                    // 请求成功
                    liveData_init.postValue(bean)

                }

                override fun onError(throwable: Throwable) {
                    //请求失败
                    liveData_init_error.postValue(throwable.message)
                }

                override fun onComplete() {}

            })
    }
}