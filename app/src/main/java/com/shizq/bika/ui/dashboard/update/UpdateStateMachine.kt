package com.shizq.bika.ui.dashboard.update

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.BuildConfig
import com.shizq.bika.core.data.repository.AppUpdateRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

private const val TAG = "UpdateSM"
private const val APK_FILE_NAME = "BIKA_update.apk"

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateStateMachine @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
) : FlowReduxStateMachineFactory<UpdateUiState, UpdateAction>() {

    /**
     * 下载进度单独通过 StateFlow 发射。
     *
     * 进入 Downloading 状态后 collectWhileInState 会监听它，
     * 并同步到主 UI 状态。
     */
    private val downloadProgress = MutableStateFlow(0f)

    /**
     * 临时保存下载请求。
     *
     * 这样可以让 HasUpdate 状态只负责切换到 Downloading，
     * 真正的下载逻辑放在 Downloading.onEnter 中执行。
     */
    private var pendingDownloadRequest: DownloadRequest? = null

    private data class DownloadRequest(
        val downloadUrl: String,
        val externalFilesDir: File,
    )

    init {
        initializeWith { UpdateUiState.Idle }

        spec {
            /**
             * ─────────────────────────────────────────────
             * Idle
             * ─────────────────────────────────────────────
             *
             * Idle 只表示空闲，不自动检查更新。
             * 检查更新必须通过 CheckUpdate 显式触发。
             */
            inState<UpdateUiState.Idle> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * Checking
             * ─────────────────────────────────────────────
             */
            inState<UpdateUiState.Checking> {
                onEnter {
                    val result = checkUpdate()

                    override { result }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * NoUpdate
             * ─────────────────────────────────────────────
             */
            inState<UpdateUiState.NoUpdate> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * HasUpdate
             * ─────────────────────────────────────────────
             */
            inState<UpdateUiState.HasUpdate> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.StartDownload> { action ->
                    pendingDownloadRequest = DownloadRequest(
                        downloadUrl = action.downloadUrl,
                        externalFilesDir = action.externalFilesDir,
                    )

                    downloadProgress.value = 0f

                    override { UpdateUiState.Downloading(progress = 0f) }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * Downloading
             * ─────────────────────────────────────────────
             *
             * 注意：
             * 当前没有实现真正的取消下载，所以不接 Reset。
             * 如果你直接 Reset 到 Idle，UI 会消失，但下载任务可能仍然在跑。
             */
            inState<UpdateUiState.Downloading> {
                onEnter {
                    val request = pendingDownloadRequest

                    if (request == null) {
                        override { UpdateUiState.Error("下载请求不存在，请重试") }
                    } else {
                        try {
                            val result = downloadApk(
                                downloadUrl = request.downloadUrl,
                                externalFilesDir = request.externalFilesDir,
                            )

                            Log.d(TAG, "download finished: $result")

                            override { result }
                        } finally {
                            pendingDownloadRequest = null
                        }
                    }
                }

                collectWhileInState(downloadProgress) { progress ->
                    override { UpdateUiState.Downloading(progress = progress) }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * Success
             * ─────────────────────────────────────────────
             */
            inState<UpdateUiState.Success> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            /**
             * ─────────────────────────────────────────────
             * Error
             * ─────────────────────────────────────────────
             */
            inState<UpdateUiState.Error> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.ShowError> { action ->
                    override { UpdateUiState.Error(action.message) }
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

            if (destFile.exists()) {
                destFile.delete()
            }

            appUpdateRepository.downloadApk(
                downloadUrl = downloadUrl,
                destFile = destFile,
                onProgress = { progress ->
                    downloadProgress.value = progress.coerceIn(0f, 1f)
                },
            )

            if (!destFile.exists() || destFile.length() <= 0L) {
                return UpdateUiState.Error("下载完成，但安装包文件异常")
            }

            downloadProgress.value = 1f

            UpdateUiState.Success(apkPath = destFile.absolutePath)
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