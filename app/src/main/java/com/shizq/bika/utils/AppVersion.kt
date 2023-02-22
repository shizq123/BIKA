package com.shizq.bika.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.shizq.bika.MyApp

class AppVersion {
    fun code(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getPackageInfo().longVersionCode
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo().versionCode.toLong()
        }
    }

    fun name(): String? {
        return getPackageInfo().versionName
    }

    private fun getPackageInfo(): PackageInfo {
        val packageManager = MyApp.contextBase.packageManager
        val packageName = MyApp.contextBase.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }
}