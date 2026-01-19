package com.shizq.bika.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.shizq.bika.core.datastore.di.DataStoreModule.DataStoreJson
import com.shizq.bika.core.datastore.model.UserCredentials
import com.shizq.bika.core.model.UserPreferences
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

internal object UserCredentialsSerializer : Serializer<UserCredentials> {
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
}

internal object UserPreferencesSerializer : Serializer<UserPreferences> {
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
}