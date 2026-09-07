package com.shizq.bika.core.network.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * 容忍服务端把整数字段返回成字符串（或反之）的宽松 Int 反序列化器。
 * 仅支持 [kotlinx.serialization.json.Json]，用在 [com.shizq.bika.core.network.model.PageData]
 * 的分页字段上。
 */
object FuzzyIntSerializer : KSerializer<Int> {
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

        return jsonPrimitive.intOrNull
            ?: throw IllegalStateException("JsonPrimitive content is not a valid integer: ${jsonPrimitive.content}")
    }
}
