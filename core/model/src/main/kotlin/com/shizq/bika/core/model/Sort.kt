package com.shizq.bika.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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