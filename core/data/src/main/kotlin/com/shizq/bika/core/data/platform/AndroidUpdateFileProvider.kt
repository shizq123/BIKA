package com.shizq.bika.core.data.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.io.File

class AndroidUpdateFileProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateFileProvider {

    override fun getApkFile(versionCode: Long): File {
        val dir = context.getExternalFilesDir(null) ?: context.cacheDir

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return File(dir, "BIKA_update_$versionCode.apk")
    }
}