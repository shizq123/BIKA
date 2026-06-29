package com.shizq.bika.core.data.platform

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

class AndroidAppVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionProvider {

    override val versionName: String
        get() {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return info.versionName ?: "0.0.0"
        }

    override val versionCode: Long
        get() {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }
}