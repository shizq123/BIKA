package com.shizq.bika.ui.download

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val tasks = downloadRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(task: DownloadTaskEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(task)
        }
    }
}
