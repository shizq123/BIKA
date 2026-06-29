package com.shizq.bika.feature.update.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.shizq.bika.feature.update.platform.AndroidApkInstaller
import kotlinx.coroutines.flow.Flow
import java.io.File

@Composable
fun UpdateEffectHandler(
    effects: Flow<UpdateUiEffect>,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val installer = AndroidApkInstaller(context.applicationContext)

        effects.collect { effect ->
            when (effect) {
                is UpdateUiEffect.InstallApk -> {
                    runCatching {
                        val apkFile = File(effect.apkPath)

                        if (!apkFile.exists()) {
                            error("安装包不存在")
                        }

                        if (installer.canRequestPackageInstalls()) {
                            installer.install(apkFile)
                        } else {
                            installer.openUnknownAppSourcesSettings()
                        }
                    }.onFailure { throwable ->
                        onError(
                            throwable.localizedMessage ?: "无法打开安装器",
                        )
                    }
                }

                is UpdateUiEffect.OpenUnknownAppSourcesSetting -> {
                    runCatching {
                        installer.openUnknownAppSourcesSettings()
                    }.onFailure { throwable ->
                        onError(
                            throwable.localizedMessage ?: "无法打开安装权限设置页",
                        )
                    }
                }

                is UpdateUiEffect.Toast -> {
                    // 如果你项目里有统一 Toast/Snackbar，可在这里接入
                }

                is UpdateUiEffect.Snackbar -> {
                    // 如果你项目里有 SnackbarHost，可在这里接入
                }
            }
        }
    }
}