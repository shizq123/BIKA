package com.shizq.bika.ui.leaderboard

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: BikaDataSource,
) : ViewModel() {
    val uiState = combine(
        getLeaderboard(TIME_H24),
        getLeaderboard(TIME_D7),
        getLeaderboard(TIME_D30),
        ::Triple
    )
        .asResult()
        .map { result ->
            when (result) {
                is Result.Error -> LeaderboardUiState.Error(
                    result.exception.message ?: "Unknown Error"
                )

                Result.Loading -> LeaderboardUiState.Loading
                is Result.Success -> {
                    val (dailyComics, weeklyComics, monthlyComics) = result.data

                    LeaderboardUiState.Success(
                        dailyList = dailyComics,
                        weeklyList = weeklyComics,
                        monthlyList = monthlyComics
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LeaderboardUiState.Loading
        )

    private fun getLeaderboard(timeType: String) = flow {
        val response = api.getLeaderboard(timeType)
        val comics = response.comics.map { it.asExternalModel() }
        emit(comics)
    }
}

private const val TIME_H24 = "H24"
private const val TIME_D7 = "D7"
private const val TIME_D30 = "D30"

sealed interface LeaderboardUiState {
    data class Success(
        val dailyList: List<Comic>,
        val weeklyList: List<Comic>,
        val monthlyList: List<Comic>,
    ) : LeaderboardUiState

    data class Error(val message: String) : LeaderboardUiState
    data object Loading : LeaderboardUiState
}