package com.shizq.bika.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.coroutine.FlowRestarter
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import com.shizq.bika.core.result.asResult
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val network: BikaDataSource,
) : ViewModel() {
    private val dashboardRestarter = FlowRestarter()
    val userChannelPreferences = userPreferencesDataSource.userData
        .map { it.channels }
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

    fun onChannelToggled(targetChannel: Channel, enable: Boolean) {
        viewModelScope.launch {
            val userPreferences = userPreferencesDataSource.userData.first()
            val currentList = userPreferences.channels

            val newList = currentList.map { item ->
                if (item.displayName == targetChannel.displayName) {
                    item.copy(isActive = enable)
                } else {
                    item
                }
            }

            userPreferencesDataSource.updateChannels(newList)
        }
    }

    fun onChannelsReordered(newOrderedList: List<Channel>) {
        viewModelScope.launch {
            userPreferencesDataSource.updateChannels(newOrderedList)
        }
    }

    // performAutoCheckIn
    // performInitialLogin
    fun onCheckIn() {
        viewModelScope.launch {
//            if (!userPreferencesDataSource.userData.first().autoCheckIn) {
//                return@launch
//            }
            network.punchIn()
        }
    }

    fun onLogin() {
        viewModelScope.launch {
            val username = SPUtil.get("username", "") as String
            val password = SPUtil.get("password", "") as String
            userCredentialsDataSource.setUsername(username)
            userCredentialsDataSource.setPassword(password)
            val loginData = network.login(username, password)

            userCredentialsDataSource.setToken(loginData.token)
            SPUtil.put("token", loginData.token)
        }
    }
}