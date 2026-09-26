package com.shizq.bika.feature.comicdetail.impl

import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import jakarta.inject.Inject
import kotlin.time.Clock

/** 将章节转换为下载任务并一次性入队，供详情页和章节选择面板复用。 */
class ComicDownloadEnqueuer @Inject constructor(
    private val downloadTaskRepository: DownloadTaskRepository,
    private val downloadScheduler: DownloadScheduler,
) {
    suspend fun enqueue(
        comicId: String,
        comicTitle: String,
        coverUrl: String,
        episodes: List<Chapter>,
    ) {
        if (episodes.isEmpty()) return

        val now = Clock.System.now()
        val tasks = episodes.map { episode ->
            DownloadTask(
                id = taskId(comicId, episode.order),
                comicId = comicId,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episode.id,
                episodeTitle = episode.title,
                episodeOrder = episode.order,
                createdAt = now,
                updatedAt = now,
            )
        }
        downloadTaskRepository.saveTasks(tasks)
        downloadScheduler.enqueueAll(tasks.map { it.id })
    }

    companion object {
        fun taskId(comicId: String, episodeOrder: Int) = "${comicId}_$episodeOrder"
    }
}
