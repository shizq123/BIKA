package com.shizq.bika.core.database.model

enum class DownloadErrorCode {
    NONE,

    /** 当前无可用网络 */
    NETWORK_UNAVAILABLE,

    /** 用户要求仅 Wi-Fi 下载，但当前不是 Wi-Fi */
    WIFI_REQUIRED,

    /** HTTP 请求失败 */
    HTTP_ERROR,

    /** 本地 IO 失败 */
    IO_ERROR,

    /** 文件系统不可用 */
    STORAGE_UNAVAILABLE,

    /** 压缩包损坏或格式非法 */
    ZIP_INVALID,

    /** 压缩包中没有图片 */
    NO_IMAGES_FOUND,

    /** 任务被取消 */
    CANCELED,

    /** 未知错误 */
    UNKNOWN,
}