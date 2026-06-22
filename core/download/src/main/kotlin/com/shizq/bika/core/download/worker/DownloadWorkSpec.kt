package com.shizq.bika.core.download.worker

object DownloadWorkSpec {
    const val KEY_TASK_ID = "download_task_id"

    const val TAG_CHAPTER_DOWNLOAD = "chapter_download"

    fun uniqueWorkName(taskId: String): String =
        "chapter_download:$taskId"

    fun taskTag(taskId: String): String =
        "chapter_download_task:$taskId"
}