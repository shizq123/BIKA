package com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore.model.UserCredentials
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class UserCredentialsDataSource @Inject constructor(
    private val userCredentials: DataStore<UserCredentials>,
) {
    val userData: Flow<UserCredentials> = userCredentials.data
    suspend fun setToken(token: String?) {
        userCredentials.updateData {
            it.copy(token = token)
        }
    }

    suspend fun setUsername(username: String?) {
        userCredentials.updateData {
            it.copy(username = username)
        }
    }

    suspend fun setPassword(password: String?) {
        userCredentials.updateData {
            it.copy(password = password)
        }
    }
}