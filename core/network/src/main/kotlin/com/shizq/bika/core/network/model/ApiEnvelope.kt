package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bika API 统一响应信封：`{ code, message, data }`。 */
@Serializable
internal data class ApiEnvelope<T>(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: T? = null
)
