package com.shizq.bika.core.network.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 用户性别
 */
enum class Gender(val value: String) {
    MALE("绅士"),
    FEMALE("淑女"),
    BOT("机器人"),
    UNKNOWN("未知")
}

object GenderSerializer : KSerializer<Gender> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Gender", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Gender) {
        throw UnsupportedOperationException("Gender cannot be serialized")
    }

    override fun deserialize(decoder: Decoder): Gender {
        return when (decoder.decodeString()) {
            "m" -> Gender.MALE
            "f" -> Gender.FEMALE
            "bot" -> Gender.BOT
            else -> Gender.UNKNOWN
        }
    }
}