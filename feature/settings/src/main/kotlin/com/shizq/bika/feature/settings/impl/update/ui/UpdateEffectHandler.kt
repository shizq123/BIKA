package com.shizq.bika.feature.settings.impl.update.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.shizq.bika.feature.settings.impl.update.platform.AndroidApkInstaller
import kotlinx.coroutines.flow.Flow
import java.io.File

@Composable
fun UpdateEffectHandler(
    effects: Flow<UpdateUiEffect>,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current

    var pendingApkPath by rememberSaveable { mutableStateOf<String?>(null) }
    val reportError by rememberUpdatedState(onError)

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effects, lifecycleOwner, context) {
        val installer = AndroidApkInstaller(context.applicationContext)

        fun installOrRequestPermission(apkPath: String) {
            runCatching {
                val apkFile = File(apkPath)
                check(apkFile.exists()) { "安装包不存在" }
                if (installer.canRequestPackageInstalls()) {
                    installer.install(apkFile)
                    pendingApkPath = null
                } else {
                    pendingApkPath = apkPath
                    installer.openUnknownAppSourcesSettings()
                }
            }.onFailure { throwable ->
                pendingApkPath = null
                reportError(throwable.localizedMessage ?: "无法打开安装器")
            }
        }

        // 安装界面只在应用回到前台时打开，后台下载完成的事件由发送器保留。
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val apkPath = pendingApkPath
            if (apkPath != null && installer.canRequestPackageInstalls()) {
                installOrRequestPermission(apkPath)
            }

            effects.collect { effect ->
                when (effect) {
                    is UpdateUiEffect.InstallApk -> {
                        installOrRequestPermission(effect.apkPath)
                    }

                    is UpdateUiEffect.OpenUnknownAppSourcesSetting -> {
                        installOrRequestPermission(effect.apkPath)
                    }
                }
            }
        }
    }
}
