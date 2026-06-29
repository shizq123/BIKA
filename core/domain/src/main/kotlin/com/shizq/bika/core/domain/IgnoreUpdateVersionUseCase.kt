package com.shizq.bika.core.domain

import com.shizq.bika.core.datastore.UpdatePreferenceDataSource
import com.shizq.bika.core.network.model.AppUpdateRelease
import javax.inject.Inject

class IgnoreUpdateVersionUseCase @Inject constructor(
    private val updatePreferenceDataSource: UpdatePreferenceDataSource,
) {

    suspend operator fun invoke(release: AppUpdateRelease) {
        updatePreferenceDataSource.setIgnoredVersionCode(release.versionCode)
    }
}