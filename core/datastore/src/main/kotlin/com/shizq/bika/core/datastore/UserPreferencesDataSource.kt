package com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.UserPreferences

class UserPreferencesDataSource(
    private val userPreferences: DataStore<UserPreferences>,
) {
}