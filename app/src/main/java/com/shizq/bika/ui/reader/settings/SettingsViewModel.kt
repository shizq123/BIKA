package com.shizq.bika.ui.reader.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    val uiState = userPreferencesDataSource.userData
        .map { SettingsUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState.Loading
        )

    fun setTapZoneLayout(layout: TapZoneLayout) {
        viewModelScope.launch {
            userPreferencesDataSource.setTapZoneLayout(layout)
        }
    }
    fun setVolumeKeyNavigation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setIsVolumeKeyNavigation(enabled)
        }
    }
    fun updatePreloadCount(count: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.setPreloadCount(count)
        }
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val userData: UserPreferences) : SettingsUiState
}