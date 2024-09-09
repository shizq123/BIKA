package com.shizq.bika.ui.comiclist

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ComicListBean
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.database.Search
import com.shizq.bika.database.SearchRepository
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ComicListViewModel(application: Application) : BaseViewModel(application) {
    var tag: String? = null
    var startpage = 0//起始页数，用于跳转页数后判断当前页数
    var page = 0//当前页数
    var pages = 1//总页数
    var limit = 20//每页显示多少
    var sort: String = "dd"
    var title: String? = null
    var value: String? = null

////    var baseObservable: Observable<BaseResponse<ComicListBean>>? = null
//
//    val liveData: MutableLiveData<BaseResponse<ComicListBean>> by lazy {
//        MutableLiveData<BaseResponse<ComicListBean>>()
//    }
//    val liveData2: MutableLiveData<BaseResponse<ComicListBean2>> by lazy {
//        MutableLiveData<BaseResponse<ComicListBean2>>()
//    }

    private val repository = ComicListRepository()
    private val _comicList = MutableStateFlow<Result<ComicListBean>?>(null)
    val comicList: StateFlow<Result<ComicListBean>?> = _comicList
    private val _comicList2 = MutableStateFlow<Result<ComicListBean2>?>(null)
    val comicList2: StateFlow<Result<ComicListBean2>?> = _comicList2

    fun getComicList() {
        page++//每次请求加1
        viewModelScope.launch {
            repository.getComicListFlow(tag.toString(),sort,page,value.toString()).collect {
                _comicList.value = it
            }
        }
    }

    fun getRandom() {
        viewModelScope.launch {
            repository.getRandomFlow().collect {
                _comicList2.value = it
            }
        }
    }

    private val searchRepository: SearchRepository = SearchRepository(application)
    fun insertSearch(vararg search: Search?) {
        searchRepository.insertSearch(*search)
    }
}