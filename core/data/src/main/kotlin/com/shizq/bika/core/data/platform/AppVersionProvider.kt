package com.shizq.bika.core.data.platform

interface AppVersionProvider {
    val versionName: String
    val versionCode: Long
}