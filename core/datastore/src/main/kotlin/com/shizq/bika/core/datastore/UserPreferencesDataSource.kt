package com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TouchArea
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


    suspend fun setTouchArea(area: TouchArea) {
        userPreferences.updateData { currentPreferences ->
            currentPreferences.copy(touchArea = area)
        }
    }
}