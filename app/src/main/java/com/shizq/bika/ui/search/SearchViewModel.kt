package com.shizq.bika.ui.search

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.KeywordsBean
import com.shizq.bika.database.BikaDatabase
import com.shizq.bika.database.model.SearchEntity
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseObserver
import com.shizq.bika.network.base.BaseResponse
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : BaseViewModel(application) {
    val liveDataSearchKey: MutableLiveData<BaseResponse<KeywordsBean>> by lazy {
        MutableLiveData<BaseResponse<KeywordsBean>>()
    }

    fun getKey() {
        val headers = BaseHeaders("keywords", "GET").getHeaderMapAndToken()

        RetrofitUtil.service.keywordsGet(headers)
            .doOnSubscribe(this@SearchViewModel)
            .subscribe(object : BaseObserver<KeywordsBean>() {

                override fun onSuccess(baseResponse: BaseResponse<KeywordsBean>) {
                    liveDataSearchKey.postValue(baseResponse)
                }

                override fun onCodeError(baseResponse: BaseResponse<KeywordsBean>) {
                    liveDataSearchKey.postValue(baseResponse)
                }
            })
    }

    private val searchRepository = BikaDatabase(application).searchDao()
    val allSearchLive: LiveData<List<String>>
        get() = searchRepository.allSearchLive

    fun insertSearch(vararg searchEntities: SearchEntity) {
        viewModelScope.launch {
            searchRepository.insertSearch(*searchEntities)
        }
    }

    fun deleteSearch(vararg searchEntities: SearchEntity) {
        viewModelScope.launch {
            searchRepository.deleteSearch(*searchEntities)
        }
    }

    fun deleteAllSearch() {
        viewModelScope.launch {
            searchRepository.deleteAllSearch()
        }
    }
}