package com.shizq.bika.ui.history

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.database.BikaDatabase
import com.shizq.bika.database.model.HistoryEntity
import kotlinx.coroutines.launch

class HistoryViewModel (application: Application) : BaseViewModel(application) {
    var page=0

    private val historyDao = BikaDatabase(application).historyDao()

    //查询第一页
    val firstPageHistoryEntityLive: LiveData<List<HistoryEntity>>
        get() = historyDao.firstPageHistoryEntityLive

    //分页查询 （未验证能不能用）
  suspend  fun getAllHistory(): List<HistoryEntity>{
        page += 20
        return historyDao.gatAllHistory(page)
    }

    fun deleteHistory(vararg historyEntity: HistoryEntity) {
        viewModelScope.launch {
            historyDao.deleteHistory(*historyEntity)
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            historyDao.deleteAllHistory()
        }
    }
}