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


@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
) : ViewModel() {
    private val dashboardRestarter = FlowRestarter()
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
    fun onCheckIn() {
        viewModelScope.launch {
//            if (!userPreferencesDataSource.userData.first().autoCheckIn) {
//                return@launch
//            }
            try {
                network.punchIn()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "签到失败", e)
            }
        }
    }
}