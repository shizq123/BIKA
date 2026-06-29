package com.shizq.bika.feature.update.platform

import java.io.File

interface ApkInstaller {
    fun canRequestPackageInstalls(): Boolean
    fun install(apkFile: File)
    fun openUnknownAppSourcesSettings()
}