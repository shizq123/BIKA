package com.shizq.bika.ui.comiclist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Sort
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val api: BikaDataSource,
) : ViewModel() {
    private val tag = savedStateHandle.getStateFlow("tag", "")
    private val value = savedStateHandle.getStateFlow("value", "")

    init {
        viewModelScope.launch {
            value.collect {
                val result = api.searchComics(topic = it, sort = Sort.NEWEST, page = 1)
                println("ViewModel: $result")
            }
        }
    }
}