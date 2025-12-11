package com.shizq.bika.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.model.Comic
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val api: BikaDataSource,
) : ViewModel() {
    val uiState = flow { emit(api.getCollections()) }
        .asResult()
        .map { result ->
            when (result) {
                Result.Loading -> CollectionUiState.Loading
                is Result.Error -> CollectionUiState.Error(
                    result.exception.message ?: "加载失败，请重试"
                )

                is Result.Success -> {
                    val comics = result.data.collections
                        .flatMap { it.comics }
                        .map { it.asExternalModel() }
                    CollectionUiState.Success(comics)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CollectionUiState.Loading
        )
}

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data class Error(val message: String) : CollectionUiState
    data class Success(val comics: List<Comic>) : CollectionUiState
}