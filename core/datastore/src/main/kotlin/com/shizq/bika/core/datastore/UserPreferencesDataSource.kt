package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.NetworkLine
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.core.model.UserPreferences
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData: Flow<UserPreferences> = userPreferences.data

    suspend fun setReadingMode(mode: ReadingMode) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(readingMode = mode)
        }
    }

    suspend fun setScreenOrientation(orientation: ScreenOrientation) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(screenOrientation = orientation)
        }
    }

    suspend fun setTapZoneLayout(layout: TapZoneLayout) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(tapZoneLayout = layout)
        }
    }

    suspend fun setIsVolumeKeyNavigation(enabled: Boolean) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(volumeKeyNavigation = enabled)
        }
    }

    suspend fun updateChannels(channels: List<Channel>) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(channels = channels)
        }
    }

    suspend fun setPreloadCount(count: Int) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(preloadCount = count)
        }
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        userPreferences.updateData {
            it.copy(darkThemeConfig = config)
        }
    }

    suspend fun setNetworkLine(line: NetworkLine) {
        userPreferences.updateData {
            it.copy(selectedNetworkLine = line)
        }
    }

    suspend fun setAutoCheckIn(enabled: Boolean) {
        userPreferences.updateData {
            it.copy(autoCheckIn = enabled)
        }
    }

    suspend fun setDns(dns: Set<String>) {
        userPreferences.updateData {
            it.copy(dns = it.dns + dns)
        }
    }
}