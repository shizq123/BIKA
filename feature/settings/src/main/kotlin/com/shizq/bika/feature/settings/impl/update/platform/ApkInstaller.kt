package com.shizq.bika.feature.settings.impl.update.platform

import java.io.File

interface ApkInstaller {
    fun canRequestPackageInstalls(): Boolean
    fun install(apkFile: File)
    fun openUnknownAppSourcesSettings()
}