package com.shizq.bika.ui.leaderboard

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.model.User
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.util.injectLocalStatusFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.coroutine.restartable

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
) : ViewModel() {
    private val leaderboardRestarter = FlowRestarter()

    val scrollStates = List(4) { LazyListState() }

    // 网络数据：一次性加载（排行榜数据本身不需要轮询）
    private val rawLeaderboardFlow = combine(
        getLeaderboard(TIME_H24),
        getLeaderboard(TIME_D7),
        getLeaderboard(TIME_D30),
        getKnightLeaderboardFlow(),
    ) { daily, weekly, monthly, knights ->
        AllLeaderboards(
            dailyComics = daily,
            weeklyComics = weekly,
            monthlyComics = monthly,
            knightUsers = knights
        )
    }

    val leaderboardUiState = combine(
        rawLeaderboardFlow,
        // DB Flow：收藏/阅读进度任何变化都会触发此流发射新值，驱动 UI 实时更新
        historyDao.getDetailedHistories(),
    ) { allData, histories ->
        AllLeaderboards(
            dailyComics = allData.dailyComics.injectLocalStatusFrom(histories),
            weeklyComics = allData.weeklyComics.injectLocalStatusFrom(histories),
            monthlyComics = allData.monthlyComics.injectLocalStatusFrom(histories),
            knightUsers = allData.knightUsers,
        )
    }
        .asResult()
        .restartable(leaderboardRestarter)
        .map { result ->
            when (result) {
                is Result.Error -> LeaderboardUiState.Error(
                    result.exception.message ?: "Unknown Error"
                )

                Result.Loading -> LeaderboardUiState.Loading
                is Result.Success -> {
                    val allData = result.data
                    LeaderboardUiState.Success(
                        dailyList = allData.dailyComics,
                        weeklyList = allData.weeklyComics,
                        monthlyList = allData.monthlyComics,
                        knightList = allData.knightUsers
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LeaderboardUiState.Loading
        )

    fun refresh() {
        leaderboardRestarter.restart()
    }

    private fun getKnightLeaderboardFlow() = flow {
        val response = api.getKnightLeaderboard()
        val users = response.users.map { it.asExternalModel() }
        emit(users)
    }

    private fun getLeaderboard(timeType: String) = flow {
        val response = api.getLeaderboard(timeType)
        // 这里只发射原始网络数据，本地状态由上层 combine + DB Flow 注入
        emit(response.comics)
    }

    private data class AllLeaderboards(
        val dailyComics: List<ComicSimple>,
        val weeklyComics: List<ComicSimple>,
        val monthlyComics: List<ComicSimple>,
        val knightUsers: List<User>
    )
}

private const val TIME_H24 = "H24"
private const val TIME_D7 = "D7"
private const val TIME_D30 = "D30"

sealed interface LeaderboardUiState {
    data class Success(
        val dailyList: List<ComicSimple>,
        val weeklyList: List<ComicSimple>,
        val monthlyList: List<ComicSimple>,
        val knightList: List<User>
    ) : LeaderboardUiState

    data class Error(val message: String) : LeaderboardUiState
    data object Loading : LeaderboardUiState
}