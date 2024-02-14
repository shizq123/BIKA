package com.shizq.bika.ui.apps

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatRoomListOldBean
import com.shizq.bika.bean.PicaAppsBean
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppsFragmentViewModel(application: Application) : BaseViewModel(application) {
    private val repository = AppsRepository()

    private val _roomList = MutableStateFlow<Result<ChatRoomListOldBean>?>(null)
    val roomList: StateFlow<Result<ChatRoomListOldBean>?> = _roomList
    private val _appsFlow = MutableStateFlow<Result<PicaAppsBean>?>(null)
    val appsFlow: StateFlow<Result<PicaAppsBean>?> = _appsFlow

    fun getChatList() {
        viewModelScope.launch {
            repository.getRoomListFlow().collect {
                _roomList.value = it
            }
        }
    }

    fun getPicaApps() {
        viewModelScope.launch {
            repository.getAppsFlow().collect { apps ->
                _appsFlow.value = apps
            }
        }

    }
}