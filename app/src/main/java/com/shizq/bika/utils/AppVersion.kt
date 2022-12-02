package com.shizq.bika.utils

import android.os.Build
import com.shizq.bika.MyApp

class AppVersion {
    fun code(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            MyApp.contextBase.packageManager.getPackageInfo(
                MyApp.contextBase.packageName.toString(),
                0
            ).longVersionCode
        } else {
            MyApp.contextBase.packageManager.getPackageInfo(
                MyApp.contextBase.packageName.toString(),
                0
            ).versionCode.toLong()
        }
    }

    fun name(): String? {
        return MyApp.contextBase.packageManager.getPackageInfo(
            MyApp.contextBase.packageName.toString(),
            0
        )?.versionName
    }
}