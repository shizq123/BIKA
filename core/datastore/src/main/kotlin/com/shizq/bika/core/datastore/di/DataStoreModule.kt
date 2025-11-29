package com.shizq.bika.core.datastore.di

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.model.UserCredentials
import com.shizq.bika.core.model.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    val DataStoreJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        allowSpecialFloatingPointValues = true
    }

    @Provides
    @Singleton
    internal fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UserPreferences> =
        DataStoreFactory.create(
            serializer = object : Serializer<UserPreferences> {
                override suspend fun readFrom(input: InputStream): UserPreferences {
                    try {
                        return DataStoreJson.decodeFromStream(UserPreferences.serializer(), input)
                    } catch (e: Exception) {
                        throw CorruptionException("Failed to decode data", e)
                    }
                }

                override suspend fun writeTo(
                    t: UserPreferences,
                    output: OutputStream
                ) {
                    DataStoreJson.encodeToStream(UserPreferences.serializer(), t, output)
                }

                override val defaultValue: UserPreferences
                    get() = UserPreferences()
            },
            scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
        ) {
            context.dataStoreFile("user_preferences")
        }

    @Provides
    @Singleton
    internal fun providesUserCredentialsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UserCredentials> =
        DataStoreFactory.create(
            serializer = object : Serializer<UserCredentials> {
                override suspend fun readFrom(input: InputStream): UserCredentials {
                    try {
                        return DataStoreJson.decodeFromStream(UserCredentials.serializer(), input)
                    } catch (e: Exception) {
                        throw CorruptionException("Failed to decode data", e)
                    }
                }

                override suspend fun writeTo(
                    t: UserCredentials,
                    output: OutputStream
                ) {
                    DataStoreJson.encodeToStream(UserCredentials.serializer(), t, output)
                }

                override val defaultValue: UserCredentials
                    get() = UserCredentials()
            },
            scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
        ) {
            context.dataStoreFile("user_credentials")
        }
}