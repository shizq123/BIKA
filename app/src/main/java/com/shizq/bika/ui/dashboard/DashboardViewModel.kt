package com.shizq.bika.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


import com.shizq.bika.core.coroutine.restartable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface CheckInEvent {
    data class Success(val message: String) : CheckInEvent
    data class Error(val error: String) : CheckInEvent
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
) : ViewModel() {
    private val dashboardRestarter = FlowRestarter()

    private val _checkInEvent = MutableSharedFlow<CheckInEvent>()
    val checkInEvent = _checkInEvent.asSharedFlow()

    val userChannelPreferences = userPreferencesDataSource.userData
        .map { preferences ->
            preferences.channels.filter { it.isActive }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )
    val userProfileUiState = flow {
        emit(network.fetchUserProfile())
    }.asResult()
        .restartable(dashboardRestarter)
        .map { result ->
            when (result) {
                Result.Loading -> UserProfileUiState.Loading
                is Result.Error -> UserProfileUiState.Error(result.exception.message ?: "")
                is Result.Success -> {
                    val user = result.data.user
                    UserProfileUiState.Success(
                        User(
                            name = user.name,
                            avatarUrl = user.imageUrl,
                            characters = user.characters,
                            level = user.level,
                            exp = user.exp,
                            title = user.title,
                            gender = user.gender,
                            slogan = user.slogan,
                            hasCheckedIn = user.isPunched,
                        )
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfileUiState.Loading
        )

    fun restart() {
        dashboardRestarter.restart()
    }

    // performAutoCheckIn
    // performInitialLogin
    fun onCheckIn(isAuto: Boolean = false) {
        viewModelScope.launch {
//            if (!userPreferencesDataSource.userData.first().autoCheckIn) {
//                return@launch
//            }
            try {
                network.punchIn()
                val msg = if (isAuto) "自动签到成功！已成功打哔咔。" else "打卡成功！已成功打哔咔。"
                _checkInEvent.emit(CheckInEvent.Success(msg))
                restart()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "签到失败", e)
                _checkInEvent.emit(CheckInEvent.Error("打卡失败：${e.localizedMessage ?: "未知错误"}"))
            }
        }
    }
}