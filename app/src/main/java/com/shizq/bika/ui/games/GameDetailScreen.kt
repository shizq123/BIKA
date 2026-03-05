package com.shizq.bika.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.GameDetails
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    GameDetailContent()
}

@Composable
fun GameDetailContent() {
    Scaffold(
        topBar = {

        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {

        }
    }
}

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    @Assisted private val id: String,
) : ViewModel() {
    val uiState = flow { emit(api.getGameDetail(id)) }
        .asResult()
        .map { result ->
            when (result) {
                is Result.Error -> GameDetailUiState.Error(result.exception.message.toString())
                Result.Loading -> GameDetailUiState.Loading
                is Result.Success -> GameDetailUiState.Success(result.data.details)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameDetailUiState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(id: String): GameDetailViewModel
    }
}

sealed interface GameDetailUiState {
    data class Success(val details: GameDetails) : GameDetailUiState
    data class Error(val message: String) : GameDetailUiState
    data object Loading : GameDetailUiState
}