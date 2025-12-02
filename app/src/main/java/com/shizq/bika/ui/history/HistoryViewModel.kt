package com.shizq.bika.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.database.dao.HistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao,
) : ViewModel() {
    val historiesWithReadChapters = historyDao.getHistoriesWithReadChapters()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun clearHistory() {

    }

    fun clearAllHistory() {
        viewModelScope.launch {
        }
    }
}