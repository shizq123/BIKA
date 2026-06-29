package com.shizq.bika.feature.update.model

sealed interface UpdateError {
    data object NetworkUnavailable : UpdateError
    data object ServerError : UpdateError
    data object StorageUnavailable : UpdateError
    data object InvalidDownloadUrl : UpdateError
    data object InvalidApkFile : UpdateError
    data object ApkHashMismatch : UpdateError
    data object InstallPermissionDenied : UpdateError

    data class Unknown(
        val throwable: Throwable,
    ) : UpdateError
}

fun UpdateError.toUserMessage(): String {
    return when (this) {
        UpdateError.NetworkUnavailable -> "网络不可用，请检查网络后重试"
        UpdateError.ServerError -> "服务器异常，请稍后重试"
        UpdateError.StorageUnavailable -> "无法访问存储空间"
        UpdateError.InvalidDownloadUrl -> "更新包下载地址异常"
        UpdateError.InvalidApkFile -> "安装包文件异常，请重新下载"
        UpdateError.ApkHashMismatch -> "安装包完整性校验失败，请重新下载"
        UpdateError.InstallPermissionDenied -> "请允许应用安装未知来源应用"
        is UpdateError.Unknown -> throwable.localizedMessage ?: "未知错误"
    }
}