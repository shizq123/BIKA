package com.shizq.bika.ui.leaderboard

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.KnightBean
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse

class LeaderboardKnightViewModel(application: Application) : BaseViewModel(application) {
    val liveData: MutableLiveData<BaseResponse<KnightBean>> by lazy {
        MutableLiveData<BaseResponse<KnightBean>>()
    }

    fun getKnight() {
        RetrofitUtil.service.knightGet(
            BaseHeaders("comics/knight-leaderboard", "GET").getHeaderMapAndToken()
        )
            .doOnSubscribe(this@LeaderboardKnightViewModel)
            .subscribe(object : BaseObserver<KnightBean>() {
                override fun onSuccess(baseResponse: BaseResponse<KnightBean>) {
                    // 请求成功
                    liveData.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<KnightBean>) {
                    liveData.postValue(baseResponse)
                }

            })
    }
}