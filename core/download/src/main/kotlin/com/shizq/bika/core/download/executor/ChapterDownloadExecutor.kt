package com.shizq.bika.core.download.executor

import com.shizq.bika.core.download.model.DownloadTask

interface ChapterDownloadExecutor {
    suspend fun execute(
        task: DownloadTask,
        workerToken: String,
    ): ChapterDownloadResult
}