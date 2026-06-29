package com.shizq.bika.core.data.platform

import java.io.File

interface UpdateFileProvider {
    fun getApkFile(versionCode: Long): File
}