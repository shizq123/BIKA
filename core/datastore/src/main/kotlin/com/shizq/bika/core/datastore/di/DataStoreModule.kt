package com.shizq.bika.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.model.UpdatePreference
import com.shizq.bika.core.datastore.model.UserCredentials
import com.shizq.bika.core.datastore.serializer.UpdatePreferencesSerializer
import com.shizq.bika.core.datastore.serializer.UserCredentialsSerializer
import com.shizq.bika.core.datastore.serializer.UserPreferencesSerializer
import com.shizq.bika.core.datastore.serializer.UserProfileCacheMigration
import com.shizq.bika.core.datastore.serializer.UserProfileCacheSerializer
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Provider
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

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
            serializer = UserPreferencesSerializer,
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
            serializer = UserCredentialsSerializer,
            scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
        ) {
            context.dataStoreFile("user_credential")
        }

    @Provides
    @Singleton
    internal fun providesUserProfileCacheDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        legacyPreferences: Provider<DataStore<UserPreferences>>,
    ): DataStore<UserProfileSnapshot> =
        DataStoreFactory.create(
            serializer = UserProfileCacheSerializer,
            scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
            migrations = listOf(UserProfileCacheMigration(legacyPreferences::get)),
        ) {
            context.dataStoreFile("user_profile_cache")
        }

    @Provides
    @Singleton
    internal fun providesUpdatePreferenceDataSource(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UpdatePreference> =
        DataStoreFactory.create(
            serializer = UpdatePreferencesSerializer,
            scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
        ) {
            context.dataStoreFile("update_preferences")
        }
}