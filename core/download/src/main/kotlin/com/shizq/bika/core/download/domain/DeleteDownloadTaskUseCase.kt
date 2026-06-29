package com.shizq.bika.core.download.domain

import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.core.download.storage.LocalComicStorage
import jakarta.inject.Inject
import java.io.File

class DeleteDownloadTaskUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val scheduler: DownloadScheduler,
    private val storage: LocalComicStorage,
) {

    companion object {
        private const val STOP_TIMEOUT_MS = 15_000L
    }

    suspend operator fun invoke(taskId: String): DeleteDownloadTaskResult {
        val task = repository.getTask(taskId)
            ?: return DeleteDownloadTaskResult.NotFound

        // 先写保护状态，避免 Worker 晚一步把任务写成 COMPLETED / FAILED
        repository.markCanceled(
            taskId = taskId,
            message = "用户删除任务",
        )

        val stopped = scheduler.cancelAndAwaitStopped(
            taskId = taskId,
            timeoutMs = STOP_TIMEOUT_MS,
        )

        if (!stopped) {
            runCatching {
                repository.markCanceled(
                    taskId = taskId,
                    message = "任务正在停止中，请稍后重试删除",
                )
            }
            return DeleteDownloadTaskResult.Failed(
                reason = DeleteDownloadTaskResult.Failed.Reason.CANCEL_TIMEOUT,
                message = "下载任务未能在限定时间内停止，请稍后重试",
            )
        }

        val episodeDir = resolveEpisodeDir(task.localPath, task.comicId, task.episodeOrder)

        if (episodeDir.exists()) {
            val deleted = try {
                storage.deleteEpisodeDir(episodeDir)
            } catch (_: Throwable) {
                false
            }

            if (!deleted) {
                runCatching {
                    repository.markCanceled(
                        taskId = taskId,
                        message = "下载已取消，但本地文件删除失败，请稍后重试",
                    )
                }
                return DeleteDownloadTaskResult.Failed(
                    reason = DeleteDownloadTaskResult.Failed.Reason.FILE_DELETE_FAILED,
                    message = "本地文件删除失败",
                )
            }
        }

        return try {
            repository.deleteTask(taskId)
            DeleteDownloadTaskResult.Deleted
        } catch (_: Throwable) {
            runCatching {
                repository.markCanceled(
                    taskId = taskId,
                    message = "本地文件已删除，但任务记录删除失败，请稍后重试",
                )
            }
            DeleteDownloadTaskResult.Failed(
                reason = DeleteDownloadTaskResult.Failed.Reason.DATABASE_DELETE_FAILED,
                message = "任务记录删除失败，请稍后重试",
            )
        }
    }

    private fun resolveEpisodeDir(
        localPath: String,
        comicId: String,
        episodeOrder: Int,
    ): File {
        val path = localPath.trim()
        return if (path.isNotEmpty()) {
            File(path)
        } else {
            storage.resolveEpisodeDir(
                comicId = comicId,
                episodeOrder = episodeOrder,
            )
        }
    }
}