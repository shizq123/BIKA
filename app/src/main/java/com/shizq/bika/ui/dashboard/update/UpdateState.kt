package com.shizq.bika.ui.dashboard.update

import java.io.File

sealed interface UpdateUiState {
    /** 初始重置态 */
    data object Idle : UpdateUiState

    /** 正在检查更新 */
    data object Checking : UpdateUiState

    /** 已检查，当前没有新版本 */
    data object NoUpdate : UpdateUiState

    /** 有新版本 */
    data class HasUpdate(
        val remoteVersion: String,
        val changelog: String,
        val downloadUrl: String,
    ) : UpdateUiState

    /** 正在下载 */
    data class Downloading(
        val progress: Float,
    ) : UpdateUiState

    /** 下载成功 */
    data class Success(
        val apkPath: String,
    ) : UpdateUiState

    /** 检查或下载失败 */
    data class Error(
        val message: String,
    ) : UpdateUiState
}

sealed interface UpdateAction {
    /** 用户手动重试/重新检查 */
    data object CheckUpdate : UpdateAction

    /** 用户点击立即更新 */
    data class StartDownload(
        val downloadUrl: String,
        val externalFilesDir: File,
    ) : UpdateAction

    /** 重置状态 */
    data object Reset : UpdateAction
}