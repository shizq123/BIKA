package com.shizq.bika.core.datastore

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.shizq.bika.core.datastore.model.UserCredentials

fun credentialMigration(context: Context): SharedPreferencesMigration<UserCredentials> {
    return SharedPreferencesMigration(
        context,
        "com.shizq.bika_preferences"
    ) { prefs: SharedPreferencesView, credentials: UserCredentials ->
        credentials.copy(
            username = prefs.getString("username", null),
            password = prefs.getString("password", null),
        )
    }
}