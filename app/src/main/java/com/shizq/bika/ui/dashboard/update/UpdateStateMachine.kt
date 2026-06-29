package com.shizq.bika.ui.dashboard.update

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.BuildConfig
import com.shizq.bika.core.data.repository.AppUpdateRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "UpdateSM"
private const val APK_FILE_NAME = "BIKA_update.apk"

class UpdateStateMachine @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
) : FlowReduxStateMachineFactory<UpdateUiState, UpdateAction>() {

    /**
     * 下载进度单独通过 StateFlow 发射，
     * 仅在 Downloading 状态下 collect 并同步到主状态。
     */
    private val downloadProgress = MutableStateFlow(0f)

    init {
        initializeWith { UpdateUiState.Idle }

        spec {
            // ─────────────────────────────────────────────
            // Idle
            // ─────────────────────────────────────────────
            inState<UpdateUiState.Idle> {
                onEnter {
                    val result = checkUpdate()
                    override { result }
                }
            }

            // ─────────────────────────────────────────────
            // Checking
            // 一般不接 Reset，因为当前未实现“取消检查”
            // ─────────────────────────────────────────────
            inState<UpdateUiState.Checking> {
                // 如果后面你要支持取消检查，再加对应 Action
            }

            // ─────────────────────────────────────────────
            // NoUpdate
            // ─────────────────────────────────────────────
            inState<UpdateUiState.NoUpdate> {
                on<UpdateAction.CheckUpdate> { action ->
                    val result = checkUpdate()
                    override { result }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            // ─────────────────────────────────────────────
            // HasUpdate
            // ─────────────────────────────────────────────
            inState<UpdateUiState.HasUpdate> {
                on<UpdateAction.CheckUpdate> { action ->
                    val result = checkUpdate()
                    override { result }
                }

                on<UpdateAction.StartDownload> { action ->
                    downloadProgress.value = 0f
                    override { UpdateUiState.Downloading(0f) }

                    val result = downloadApk(
                        downloadUrl = action.downloadUrl,
                        externalFilesDir = action.externalFilesDir,
                    )
                    Log.d(TAG, "download finished: $result")
                    override { result }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            // ─────────────────────────────────────────────
            // Downloading
            // 这里只同步进度，不接 Reset
            // 因为当前并没有真正实现“取消下载”
            // 如果这里直接 Reset，UI 回 Idle，但下载任务可能还在跑
            // ─────────────────────────────────────────────
            inState<UpdateUiState.Downloading> {
                collectWhileInState(downloadProgress.asStateFlow()) { progress ->
                    override { UpdateUiState.Downloading(progress) }
                }
            }

            // ─────────────────────────────────────────────
            // Success
            // ─────────────────────────────────────────────
            inState<UpdateUiState.Success> {
                on<UpdateAction.CheckUpdate> { action ->
                    val result = checkUpdate()
                    override { result }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            // ─────────────────────────────────────────────
            // Error
            // ─────────────────────────────────────────────
            inState<UpdateUiState.Error> {
                on<UpdateAction.CheckUpdate> { action ->
                    val result = checkUpdate()
                    override { result }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }
        }
    }

    private suspend fun checkUpdate(): UpdateUiState {
        return try {
            val release = appUpdateRepository.checkForUpdate(BuildConfig.VERSION_NAME)

            if (release == null) {
                UpdateUiState.NoUpdate
            } else {
                UpdateUiState.HasUpdate(
                    remoteVersion = release.remoteVersion,
                    changelog = release.changelog,
                    downloadUrl = release.downloadUrl,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "检测版本更新失败", e)
            UpdateUiState.Error(
                message = "检测更新失败: ${e.localizedMessage ?: "网络异常"}",
            )
        }
    }

    private suspend fun downloadApk(
        downloadUrl: String,
        externalFilesDir: File,
    ): UpdateUiState {
        return try {
            if (!externalFilesDir.exists()) {
                externalFilesDir.mkdirs()
            }

            val destFile = File(externalFilesDir, APK_FILE_NAME)

            appUpdateRepository.downloadApk(
                downloadUrl = downloadUrl,
                destFile = destFile,
                onProgress = { progress ->
                    downloadProgress.value = progress.coerceIn(0f, 1f)
                },
            )

            UpdateUiState.Success(destFile.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "下载最新版安装包失败", e)
            UpdateUiState.Error(
                message = "下载更新包失败: ${e.localizedMessage ?: "网络异常"}",
            )
        }
    }
}