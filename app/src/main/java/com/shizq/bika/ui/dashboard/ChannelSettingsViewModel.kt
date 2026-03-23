package com.shizq.bika.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.map

@HiltViewModel
class ChannelSettingsViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
): ViewModel() {
    val userChannelPreferences = userPreferencesDataSource.userData
    .map { it.channels }
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )
    fun saveChannelSettings(finalChannels: List<Channel>) {
        viewModelScope.launch {
            userPreferencesDataSource.updateChannels(finalChannels)
        }
    }
}