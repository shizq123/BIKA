package com.shizq.bika.feature.update.ui

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.domain.CheckAppUpdateUseCase
import com.shizq.bika.core.domain.DownloadUpdateApkUseCase
import com.shizq.bika.core.domain.IgnoreUpdateVersionUseCase
import com.shizq.bika.core.domain.MarkUpdatePromptedUseCase
import com.shizq.bika.core.domain.ShouldShowUpdateUseCase
import com.shizq.bika.core.network.model.AppUpdateRelease
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "UpdateSM"

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateStateMachine @Inject constructor(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val shouldShowUpdateUseCase: ShouldShowUpdateUseCase,
    private val markUpdatePromptedUseCase: MarkUpdatePromptedUseCase,
    private val ignoreUpdateVersionUseCase: IgnoreUpdateVersionUseCase,
    private val downloadUpdateApkUseCase: DownloadUpdateApkUseCase,
    private val effectEmitter: UpdateEffectEmitter,
) : FlowReduxStateMachineFactory<UpdateUiState, UpdateAction>() {

    private val downloadProgress = MutableStateFlow(0f)

    init {
        initializeWith { UpdateUiState.Idle }

        spec {
            inState<UpdateUiState.Idle> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.ShowError> { action ->
                    override {
                        UpdateUiState.Error(
                            message = action.message,
                            canRetry = action.canRetry,
                            retryAction = action.retryAction,
                        )
                    }
                }
            }

            inState<UpdateUiState.Checking> {
                onEnter {
                    val result = checkUpdate(source = UpdateCheckSource.Auto)

                    override { result }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            inState<UpdateUiState.NoUpdate> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            inState<UpdateUiState.Ignored> {
                on<UpdateAction.CheckUpdate> {
                    override { UpdateUiState.Checking }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            inState<UpdateUiState.HasUpdate> {
                on<UpdateAction.StartDownload> { action ->
                    downloadProgress.value = 0f

                    override {
                        UpdateUiState.Downloading(
                            release = action.release,
                            progress = 0f,
                        )
                    }
                }

                on<UpdateAction.RemindLater> { action ->
                    if (!action.release.forceUpdate) {
                        markUpdatePromptedUseCase()
                        override { UpdateUiState.Idle }
                    } else noChange()
                }

                on<UpdateAction.IgnoreVersion> { action ->
                    if (!action.release.forceUpdate) {
                        ignoreUpdateVersionUseCase(action.release)
                        override { UpdateUiState.Idle }
                    } else noChange()
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }

            inState<UpdateUiState.Downloading> {
                onEnter {
                    val result = downloadApk(release = snapshot.release)

                    override { result }
                }

                collectWhileInState(downloadProgress) { progress ->
                    mutate { copy(progress = progress) }
                }
            }

            inState<UpdateUiState.Error> {
                on<UpdateAction.Retry> { action ->
                    when (val retry = action.retryAction) {
                        is UpdateRetryAction.CheckUpdate -> {
                            override { UpdateUiState.Checking }
                        }

                        is UpdateRetryAction.Download -> {
                            downloadProgress.value = 0f

                            override {
                                UpdateUiState.Downloading(
                                    release = retry.release,
                                    progress = 0f,
                                )
                            }
                        }
                    }
                }

                on<UpdateAction.Reset> {
                    override { UpdateUiState.Idle }
                }
            }
        }
    }

    private suspend fun checkUpdate(
        source: UpdateCheckSource,
    ): UpdateUiState {
        return try {
            val release = checkAppUpdateUseCase() ?: return UpdateUiState.NoUpdate

            val shouldShow = shouldShowUpdateUseCase(
                release = release,
                isManualCheck = source == UpdateCheckSource.Manual,
            )

            if (!shouldShow) {
                return UpdateUiState.Ignored
            }

            markUpdatePromptedUseCase()

            UpdateUiState.HasUpdate(release = release)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "检查更新失败", e)

            UpdateUiState.Error(
                message = "检测更新失败: ${e.localizedMessage ?: "网络异常"}",
                canRetry = true,
                retryAction = UpdateRetryAction.CheckUpdate(source),
            )
        }
    }

    private suspend fun downloadApk(
        release: AppUpdateRelease,
    ): UpdateUiState {
        return try {
            val apkFile = downloadUpdateApkUseCase(
                release = release,
                onProgress = { progress ->
                    downloadProgress.value = progress
                },
            )

            effectEmitter.emit(
                UpdateUiEffect.InstallApk(
                    apkPath = apkFile.absolutePath,
                ),
            )

            UpdateUiState.Idle
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "下载更新包失败", e)

            UpdateUiState.Error(
                message = "下载更新包失败: ${e.localizedMessage ?: "网络异常"}",
                canRetry = true,
                retryAction = UpdateRetryAction.Download(release),
            )
        }
    }
}