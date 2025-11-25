package com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore.model.UserCredentials
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserCredentialsDataSource @Inject constructor(
    private val userCredentials: DataStore<UserCredentials>,
) {
    val userData: Flow<UserCredentials> = userCredentials.data
    suspend fun setToken(token: String?) {
        userCredentials.updateData {
            it.copy(token = token)
        }
    }
}