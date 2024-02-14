package com.shizq.bika.ui.comicinfo

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ActionBean
import com.shizq.bika.bean.EpisodeBean
import com.shizq.bika.bean.ComicInfoBean
import com.shizq.bika.bean.RecommendBean
import com.shizq.bika.db.History
import com.shizq.bika.db.HistoryRepository
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ComicInfoViewModel(application: Application) : BaseViewModel(application) {
    var bookId: String = ""
    var title:String? =null
    var author:String? =null
    var totalViews:String? =null
    var episodePage = 0
    var creatorId: String = ""
    var totalEps: Int = 1
    var creator:ComicInfoBean.Comic.Creator? =null

    private val repository = ComicInfoRepository()

    private val _comicInfo = MutableStateFlow<Result<ComicInfoBean>?>(null)
    val comicInfo: StateFlow<Result<ComicInfoBean>?> = _comicInfo
    private val _episode = MutableStateFlow<Result<EpisodeBean>?>(null)
    val episode: StateFlow<Result<EpisodeBean>?> = _episode
    private val _like = MutableStateFlow<Result<ActionBean>?>(null)
    val like: StateFlow<Result<ActionBean>?> = _like
    private val _recommend = MutableStateFlow<Result<RecommendBean>?>(null)
    val recommend: StateFlow<Result<RecommendBean>?> = _recommend
    private val _favourite = MutableStateFlow<Result<ActionBean>?>(null)
    val favourite: StateFlow<Result<ActionBean>?> = _favourite
    //漫画信息
    fun getInfo() {
        viewModelScope.launch {
            repository.getInfoFlow(bookId).collect {
                _comicInfo.value = it
            }
        }
    }

    //章节
    fun getEpisode() {
        //每次页数加1
        episodePage++
        viewModelScope.launch {
            repository.getEpisodeFlow(bookId,episodePage.toString()).collect {
                _episode.value = it
            }
        }
    }

    //推荐
    fun getRecommend() {
        viewModelScope.launch {
            repository.getRecommendFlow(bookId).collect {
                _recommend.value = it
            }
        }
    }

    //爱心 喜欢
    fun getLike() {
        viewModelScope.launch {
            repository.getLikeFlow(bookId).collect {
                _like.value = it
            }
        }
    }

    //收藏
    fun getFavourite() {
        viewModelScope.launch {
            repository.getFavouriteFlow(bookId).collect {
                _favourite.value = it
            }
        }
    }

    private val historyRepository: HistoryRepository = HistoryRepository(application)

    //通过 漫画的id查询
    fun getHistory(): List<History>{
        return historyRepository.getHistory(bookId)
    }

    fun updateHistory(vararg history: History?) {
        historyRepository.updateHistory(*history)
    }
    fun insertHistory(vararg history: History?) {
        historyRepository.insertHistory(*history)
    }

}