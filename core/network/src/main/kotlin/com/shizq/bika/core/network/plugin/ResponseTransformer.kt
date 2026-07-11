package com.shizq.bika.core.network.plugin

import com.shizq.bika.core.network.model.Box
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer

val ExpectRawResponse = AttributeKey<Unit>("ExpectRawResponse")
public val ApiEnvelopePlugin: ClientPlugin<Unit> = createClientPlugin("ResponseTransformer") {
    val json = Json { ignoreUnknownKeys = true }
    transformResponseBody { response, content, requestedType ->
        if (response.request.attributes.getOrNull(ExpectRawResponse) != null) {
            return@transformResponseBody null
        }
        if (!response.status.isSuccess() || requestedType.type == Unit::class) {
            return@transformResponseBody content
        }

        val targetKotlinType = requestedType.kotlinType ?: return@transformResponseBody content

        val decodedContent = json.decodeFromStream(
            Box.serializer(serializer(targetKotlinType)),
            content.toInputStream()
        )
        if (decodedContent.code == HttpStatusCode.Unauthorized.value) {
            throw UnauthorizedException(decodedContent.message)
        }
        if (decodedContent.code != HttpStatusCode.OK.value) {
            throw ApiException(decodedContent.code, decodedContent.message)
        }
        decodedContent.data
    }
}

class ApiException(val code: Int, message: String) : Exception("API Error ($code): $message")
class UnauthorizedException(message: String) : Exception(message)