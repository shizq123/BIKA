package com.shizq.bika.core.datastore.di.com.shizq.bika.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val token: String? = null,
)
