package com.shizq.bika.ui.history

import android.app.Application
import androidx.lifecycle.LiveData
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.db.History
import com.shizq.bika.db.HistoryRepository

class HistoryViewModel (application: Application) : BaseViewModel(application) {
    private val historyRepository: HistoryRepository = HistoryRepository(application)
    val allHistoryLive: LiveData<List<History>>
        get() = historyRepository.listLiveData

    fun deleteHistory(vararg history: History?) {
        historyRepository.deleteHistory(*history)
    }

    fun deleteAllHistory() {
        historyRepository.deleteAllHistory()
    }
}