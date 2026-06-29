package com.shizq.bika.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePreference(
    val ignoredVersionCode: Long? = null,
    val lastPromptTime: Long? = null
)
