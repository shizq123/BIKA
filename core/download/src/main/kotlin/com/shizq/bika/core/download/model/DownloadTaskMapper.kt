package com.shizq.bika.core.download.model

import com.shizq.bika.core.database.model.DownloadTaskEntity

fun DownloadTaskEntity.asExternalModel(): DownloadTask =
    DownloadTask(
        id = id,
        comicId = comicId,
        comicTitle = comicTitle,
        coverUrl = coverUrl,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        episodeOrder = episodeOrder,
        status = status,
        progress = progress,
        totalPages = totalPages,
        downloadedPages = downloadedPages,
        localPath = localPath,
        isViewed = isViewed,
        priority = priority,
        errorCode = errorCode,
        errorMessage = errorMessage,
        retryCount = retryCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )

fun DownloadTask.asEntity(): DownloadTaskEntity =
    DownloadTaskEntity(
        id = id,
        comicId = comicId,
        comicTitle = comicTitle,
        coverUrl = coverUrl,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        episodeOrder = episodeOrder,
        status = status,
        progress = progress,
        totalPages = totalPages,
        downloadedPages = downloadedPages,
        localPath = localPath,
        isViewed = isViewed,
        priority = priority,
        errorCode = errorCode,
        errorMessage = errorMessage,
        retryCount = retryCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )