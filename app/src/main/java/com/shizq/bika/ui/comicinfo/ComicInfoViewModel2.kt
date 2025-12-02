package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ComicInfoViewModel2 @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val network: BikaDataSource,
) : ViewModel() {
    val comicDetailUiState = savedStateHandle.getStateFlow("id", "")
        .map { network.getComicDetails(it) }
        .asResult()
        .map { result ->
            when (result) {
                is Result.Error -> ComicDetailUiState.Error(result.exception.message.toString())
                Result.Loading -> ComicDetailUiState.Loading
                is Result.Success -> ComicDetailUiState.Success(
                    result.data.toComicDetail()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = ComicDetailUiState.Loading
        )
}
