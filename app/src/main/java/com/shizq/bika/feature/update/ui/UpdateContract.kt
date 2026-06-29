package com.shizq.bika.feature.update.ui

import com.shizq.bika.core.network.model.AppUpdateRelease

sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data object Checking : UpdateUiState

    data object NoUpdate : UpdateUiState

    data object Ignored : UpdateUiState

    data class HasUpdate(
        val release: AppUpdateRelease,
    ) : UpdateUiState

    data class Downloading(
        val release: AppUpdateRelease,
        val progress: Float,
    ) : UpdateUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = false,
        val retryAction: UpdateRetryAction? = null,
    ) : UpdateUiState
}

sealed interface UpdateAction {
    data class CheckUpdate(
        val source: UpdateCheckSource = UpdateCheckSource.Auto,
    ) : UpdateAction

    data class StartDownload(
        val release: AppUpdateRelease,
    ) : UpdateAction

    data class RemindLater(
        val release: AppUpdateRelease,
    ) : UpdateAction

    data class IgnoreVersion(
        val release: AppUpdateRelease,
    ) : UpdateAction

    data object CancelDownload : UpdateAction

    data class Retry(
        val retryAction: UpdateRetryAction,
    ) : UpdateAction

    data class ShowError(
        val message: String,
        val canRetry: Boolean = false,
        val retryAction: UpdateRetryAction? = null,
    ) : UpdateAction

    data object Reset : UpdateAction
}

sealed interface UpdateUiEffect {
    data class InstallApk(
        val apkPath: String,
    ) : UpdateUiEffect

    data class OpenUnknownAppSourcesSetting(
        val apkPath: String,
    ) : UpdateUiEffect

    data class Toast(
        val message: String,
    ) : UpdateUiEffect

    data class Snackbar(
        val message: String,
    ) : UpdateUiEffect
}

enum class UpdateCheckSource {
    Auto,
    Manual,
}

sealed interface UpdateRetryAction {
    data class CheckUpdate(
        val source: UpdateCheckSource,
    ) : UpdateRetryAction

    data class Download(
        val release: AppUpdateRelease,
    ) : UpdateRetryAction
}