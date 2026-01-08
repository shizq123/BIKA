package com.shizq.bika.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
@Parcelize
value class Sort(val value: String) : Parcelable {
    override fun toString(): String = value

    companion object {
        /** 最新 */
        val NEWEST = Sort("dd")

        /** 最老 */
        val OLDEST = Sort("da")

        /** 最多喜欢 */
        val MOST_LIKED = Sort("ld")

        /** 最多浏览 */
        val MOST_VIEWED = Sort("vd")
    }
}