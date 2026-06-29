package com.shizq.bika.core.datastore.model

data class UpdatePreference(
    val ignoredVersionCode: Long? = null,
    val lastPromptTime: Long? = null
)
