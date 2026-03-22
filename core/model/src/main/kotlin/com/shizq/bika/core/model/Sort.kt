package com.shizq.bika.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

//{"code":400,"error":"1002","message":"validation error","detail":"s must be one of [UA, CA, da, dd, ld, vd]"}
// TODO: rename to SortOrder
@Serializable
enum class Sort(val value: String, val title: String) {

    NEWEST("dd", "最新发布"),
    OLDEST("da", "最早发布"),
    MOST_LIKED("ld", "最多喜欢"),
    MOST_VIEWED("vd", "最多浏览");

    override fun toString(): String = value
}