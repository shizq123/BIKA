package com.shizq.bika.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.FavoriteTag
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardStateMachine: DashboardStateMachine,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    historyDao: ReadingHistoryDao,
) : ViewModel() {

    private val stateMachine = dashboardStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state
    fun dispatch(action: DashboardAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    // ── 不属于 StateMachine 管理的轻量级 flows ───────────────────────────

    val lastReadHistory = historyDao.getDetailedHistories()
        .map { list -> list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val userChannelPreferences = userPreferencesDataSource.userData
        .map { prefs -> prefs.channels.filter { it.isActive } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val favoriteTags: StateFlow<List<FavoriteTag>> = userPreferencesDataSource.userData
        .map { it.favoriteTags }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
