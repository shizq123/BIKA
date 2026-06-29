package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.model.UpdatePreference
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class UpdatePreferenceDataSource @Inject constructor(
    private val updatePreferences: DataStore<UpdatePreference>
) {
    val preference = updatePreferences.data
    suspend fun getIgnoredVersionCode(): Long? {
        return preference.first().ignoredVersionCode
    }

    suspend fun setIgnoredVersionCode(versionCode: Long) {
        updatePreferences.updateData {
            it.copy(ignoredVersionCode = versionCode)
        }
    }

    suspend fun clearIgnoredVersionCode() {
        updatePreferences.updateData {
            it.copy(ignoredVersionCode = null)
        }
    }

    suspend fun getLastPromptTimeMillis(): Long? {
        return preference.first().lastPromptTime
    }

    suspend fun setLastPromptTimeMillis(timeMillis: Long) {
        updatePreferences.updateData {
            it.copy(ignoredVersionCode = timeMillis)
        }
    }
}