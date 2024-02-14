package com.shizq.bika.ui.collections

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.CollectionsBean
import com.shizq.bika.network.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CollectionsViewModel(application: Application) : BaseViewModel(application) {

    private val repository =CollectionsRepository()
    private val _collections = MutableStateFlow<Result<CollectionsBean>?>(null)
    val collections: StateFlow<Result<CollectionsBean>?> = _collections

    fun getData() {
        viewModelScope.launch {
            repository.getDataFlow().collect {
                _collections.value = it
            }
        }
    }

}