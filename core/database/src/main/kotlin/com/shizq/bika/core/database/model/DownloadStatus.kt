package com.shizq.bika.core.database.model

enum class DownloadStatus {
    /** 已创建，等待调度 */
    PENDING,

    /** 当前网络条件不满足，例如仅 Wi-Fi 下载但现在不是 Wi-Fi */
    WAITING_FOR_NETWORK,

    /** 下载中 */
    DOWNLOADING,

    /** 用户手动暂停 */
    PAUSED,

    /** 已完成 */
    COMPLETED,

    /** 下载失败 */
    FAILED,

    /** 用户取消 */
    CANCELED,
    ;
}