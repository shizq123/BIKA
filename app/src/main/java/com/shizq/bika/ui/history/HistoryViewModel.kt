package com.shizq.bika.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.HistoryDao
import com.shizq.bika.core.database.model.HistoryWithReadChapters
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao,
) : ViewModel() {
    val historiesWithReadChapters = historyDao.getHistoriesWithReadChapters()
        .map { it.map(HistoryWithReadChapters::asExternalModel) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    /**
     * 请求显示“删除单条”的确认对话框。
     */
    fun requestClearHistory(comicId: String, title: String) {
        _dialogState.value = DialogState.ConfirmDeleteOne(comicId, title)
    }

    /**
     * 请求显示“清空全部”的确认对话框。
     */
    fun requestClearAllHistory() {
        _dialogState.value = DialogState.ConfirmDeleteAll
    }

    fun confirmDeletion() {
        when (val state = _dialogState.value) {
            is DialogState.ConfirmDeleteOne -> {
                viewModelScope.launch {
                    historyDao.clearHistory(state.comicId)
                }
            }

            is DialogState.ConfirmDeleteAll -> {
                viewModelScope.launch {
                    historyDao.clearAllHistory()
                }
            }

            DialogState.Hidden -> { /* Do nothing */
            }
        }
        dismissDialog()
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
    }
}

sealed interface DialogState {
    data object Hidden : DialogState
    data class ConfirmDeleteOne(val comicId: String, val title: String) : DialogState
    data object ConfirmDeleteAll : DialogState
}