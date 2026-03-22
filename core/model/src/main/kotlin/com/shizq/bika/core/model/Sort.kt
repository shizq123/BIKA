package com.shizq.bika.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
//{"code":400,"error":"1002","message":"validation error","detail":"s must be one of [UA, CA, da, dd, ld, vd]"}
@Serializable
enum class Sort(val value: String) {
    /** 最新 */
    NEWEST("dd"),

    /** 最老 */
    OLDEST("da"),

    /** 最多喜欢 */
    MOST_LIKED("ld"),

    /** 最多浏览 */
    MOST_VIEWED("vd");

    override fun toString(): String = value
}