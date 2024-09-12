package com.shizq.bika.ui.reader

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ComicsPictureBean
import com.shizq.bika.database.BikaDatabase
import com.shizq.bika.database.model.HistoryEntity
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : BaseViewModel(application) {
    var order = 1
    var bookId: String? = null
    var page = 0
    var totalEps = 1 //总章数

    val liveData_picture: MutableLiveData<BaseResponse<ComicsPictureBean>> by lazy {
        MutableLiveData<BaseResponse<ComicsPictureBean>>()
    }

    fun comicsPicture() {
        page++
        RetrofitUtil.service.comicsPictureGet(
            bookId.toString(),
            order.toString(),
            page.toString(),
            BaseHeaders("comics/$bookId/order/$order/pages?page=$page", "GET").getHeaderMapAndToken())
            .doOnSubscribe(this@ReaderViewModel)
            .subscribe(object : BaseObserver<ComicsPictureBean>() {
                override fun onSuccess(baseResponse: BaseResponse<ComicsPictureBean>) {
                    liveData_picture.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<ComicsPictureBean>) {
                    page--
                    liveData_picture.postValue(baseResponse)
                }

            })
    }

    private val historyRepository = BikaDatabase(application).historyDao()
    //通过 漫画的id查询
    suspend fun getHistory(): List<HistoryEntity>{
        return historyRepository.gatHistory(bookId!!)
    }

    fun updateHistory(vararg historyEntity: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.updateHistory(*historyEntity)
        }
    }

}