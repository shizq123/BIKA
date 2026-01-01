package com.shizq.bika.core.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class PageData<T>(
    @Serializable(with = FuzzyIntSerializer::class) val total: Int,
    @Serializable(with = FuzzyIntSerializer::class) val limit: Int,
    @Serializable(with = FuzzyIntSerializer::class) val page: Int,
    @Serializable(with = FuzzyIntSerializer::class) val pages: Int,
    val docs: List<T>,
)

private object FuzzyIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FuzzyInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer can only be used with Json")

        val jsonPrimitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: throw IllegalStateException("Expected a JsonPrimitive")

        return jsonPrimitive.intOrNull ?:
            throw IllegalStateException("JsonPrimitive content is not a valid integer: ${jsonPrimitive.content}")
    }
}