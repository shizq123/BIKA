package com.shizq.bika.core.domain

import com.shizq.bika.core.datastore.UpdatePreferenceDataSource
import com.shizq.bika.core.network.model.AppUpdateRelease
import javax.inject.Inject

class ShouldShowUpdateUseCase @Inject constructor(
    private val updatePreferenceDataSource: UpdatePreferenceDataSource,
) {

    suspend operator fun invoke(
        release: AppUpdateRelease,
        isManualCheck: Boolean,
    ): Boolean {
        if (isManualCheck) {
            return true
        }

        if (release.forceUpdate) {
            return true
        }

        val ignoredVersionCode = updatePreferenceDataSource.getIgnoredVersionCode()

        if (ignoredVersionCode == release.versionCode) {
            return false
        }

        val lastPromptTime = updatePreferenceDataSource.getLastPromptTimeMillis()

        if (lastPromptTime != null) {
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            if (now - lastPromptTime < oneDayMillis) {
                return false
            }
        }

        return true
    }
}