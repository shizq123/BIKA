package com.shizq.bika.ui.comiclist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val api: BikaDataSource,
) : ViewModel() {
    private val tag = savedStateHandle.getStateFlow("tag", "")
    private val value = savedStateHandle.getStateFlow("value", "")
}