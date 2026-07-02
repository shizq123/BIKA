@file:OptIn(ExperimentalSerializationApi::class)

package com.shizq.bika.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.shizq.bika.core.datastore.di.DataStoreModule.DataStoreJson
import com.shizq.bika.core.datastore.model.UpdatePreference
import com.shizq.bika.core.datastore.model.UserCredentials
import com.shizq.bika.core.model.preferences.UserPreferences
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject
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
            // 先解析为 JsonElement：因为 DataStoreJson 开启了 ignoreUnknownKeys，
            // 直接用新 serializer 解析旧扁平数据不会报错，而是静默丢弃全部旧字段。
            // 所以必须先探测结构，命中旧结构时走迁移。
            val element = DataStoreJson.decodeFromStream(
                kotlinx.serialization.json.JsonElement.serializer(),
                input,
            )
            val root = element.jsonObject
            return if (UserPreferencesMigration.isLegacyFlat(root)) {
                UserPreferencesMigration.migrate(DataStoreJson, root)
            } else {
                DataStoreJson.decodeFromJsonElement(UserPreferences.serializer(), element)
            }
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

internal object UpdatePreferencesSerializer : Serializer<UpdatePreference> {
    override suspend fun readFrom(input: InputStream): UpdatePreference {
        try {
            return DataStoreJson.decodeFromStream(UpdatePreference.serializer(), input)
        } catch (e: Exception) {
            throw CorruptionException("Failed to decode data", e)
        }
    }

    override suspend fun writeTo(
        t: UpdatePreference,
        output: OutputStream
    ) {
        DataStoreJson.encodeToStream(UpdatePreference.serializer(), t, output)
    }

    override val defaultValue: UpdatePreference
        get() = UpdatePreference()
}