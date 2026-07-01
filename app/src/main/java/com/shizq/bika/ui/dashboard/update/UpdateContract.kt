package com.shizq.bika.ui.dashboard.update

import java.io.File

sealed interface UpdateUiState {
    /**
     * 空闲态。
     *
     * 注意：
     * Idle 不应该自动检查更新，否则 Reset 回 Idle 会导致重新检查，
     * 用户点击“稍后”后弹窗会再次出现。
     */
    data object Idle : UpdateUiState

    /**
     * 正在检查更新。
     */
    data object Checking : UpdateUiState

    /**
     * 已检查，当前没有新版本。
     */
    data object NoUpdate : UpdateUiState

    /**
     * 有新版本。
     */
    data class HasUpdate(
        val remoteVersion: String,
        val changelog: String,
        val downloadUrl: String,
    ) : UpdateUiState

    /**
     * 正在下载。
     */
    data class Downloading(
        val progress: Float,
    ) : UpdateUiState

    /**
     * 下载成功。
     */
    data class Success(
        val apkPath: String,
    ) : UpdateUiState

    /**
     * 检查、下载或安装失败。
     */
    data class Error(
        val message: String,
    ) : UpdateUiState
}

sealed interface UpdateAction {
    /**
     * 检查更新。
     */
    data object CheckUpdate : UpdateAction

    /**
     * 用户点击立即更新。
     */
    data class StartDownload(
        val downloadUrl: String,
        val externalFilesDir: File,
    ) : UpdateAction

    /**
     * UI 层或状态机主动抛出错误。
     *
     * 例如：
     * 1. 无法获取下载目录
     * 2. FileProvider 配置异常
     * 3. 系统安装器启动失败
     */
    data class ShowError(
        val message: String,
    ) : UpdateAction

    /**
     * 重置为空闲状态。
     */
    data object Reset : UpdateAction
}