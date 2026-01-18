package com.shizq.bika

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.navigation.DashboardNavKey
import com.shizq.bika.navigation.LoginNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userCredentialsDataSource: UserCredentialsDataSource
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = userCredentialsDataSource.userData.map {
        MainActivityUiState.Success(it.token)
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val token: String?) : MainActivityUiState

    fun shouldKeepSplashScreen() = this is Loading

    fun ss(): NavKey {
        return if ((this as? Success)?.token != null) DashboardNavKey
        else LoginNavKey
    }
}