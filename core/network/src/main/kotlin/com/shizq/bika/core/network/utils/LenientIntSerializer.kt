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
 *
 * 遇到无法解析成整数的内容会抛出 [IllegalStateException]，调用方需自行捕获处理，
 * 不会静默返回默认值——避免把"数据异常"和"数据为零"混为一谈。
 */
object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

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
