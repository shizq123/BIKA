package com.shizq.bika.core.domain

import com.shizq.bika.core.datastore.UpdatePreferenceDataSource
import javax.inject.Inject

class MarkUpdatePromptedUseCase @Inject constructor(
    private val updatePreferenceDataSource: UpdatePreferenceDataSource,
) {

    suspend operator fun invoke() {
        updatePreferenceDataSource.setLastPromptTimeMillis(
            timeMillis = System.currentTimeMillis(),
        )
    }
}