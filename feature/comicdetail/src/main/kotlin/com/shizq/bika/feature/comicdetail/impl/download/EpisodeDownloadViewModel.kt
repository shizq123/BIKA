package com.shizq.bika.feature.comicdetail.impl.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportError
import com.shizq.bika.core.message.reportInfo
import com.shizq.bika.feature.comicdetail.impl.ComicDownloadEnqueuer
import com.shizq.bika.feature.comicdetail.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger("EpisodeDownloadVM")

@HiltViewModel(assistedFactory = EpisodeDownloadViewModel.Factory::class)
class EpisodeDownloadViewModel @AssistedInject constructor(
    private val chapterRepository: ChapterRepository,
    private val episodeDownloadManager: ComicDownloadEnqueuer,
    private val messageReporter: MessageReporter,
    @ApplicationContext private val context: Context,
    @Assisted("id") private val comicId: String,
    @Assisted("title") private val comicTitle: String,
    @Assisted("url") private val coverUrl: String,
) : ViewModel() {
    // TODO: 使用 flowreudx2 重构 
    private val _uiState = MutableStateFlow<EpisodeDownloadUiState>(EpisodeDownloadUiState.Initial)
    val uiState = _uiState.asStateFlow()

    fun load() {
        when (_uiState.value) {
            EpisodeDownloadUiState.Initial,
            EpisodeDownloadUiState.LoadError,
                -> Unit

            EpisodeDownloadUiState.Loading,
            EpisodeDownloadUiState.Empty,
            is EpisodeDownloadUiState.Content,
                -> return
        }

        viewModelScope.launch {
            _uiState.value = EpisodeDownloadUiState.Loading
            try {
                val episodes = chapterRepository.getAllChapters(comicId)
                _uiState.value = if (episodes.isEmpty()) {
                    EpisodeDownloadUiState.Empty
                } else {
                    EpisodeDownloadUiState.Content(
                        episodes = episodes,
                        selectedIds = episodes.map { it.id }.toSet(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "获取可选章节失败: comicId=$comicId" }
                _uiState.value = EpisodeDownloadUiState.LoadError
                Toast.makeText(context, "获取章节失败，请稍后重试", Toast.LENGTH_SHORT).show()
                messageReporter.reportError(
                    UiText.of(R.string.download_fetch_chapters_failed),
                    duration = MessageDuration.Long,
                )
            }
        }
    }

    fun retry() {
        if (_uiState.value is EpisodeDownloadUiState.Content) return
        _uiState.value = EpisodeDownloadUiState.Initial
        load()
    }

    fun toggleEpisode(id: String) {
        _uiState.update { state ->
            val content = state as? EpisodeDownloadUiState.Content ?: return@update state
            if (content.submission is SubmissionState.Submitting) return@update state

            val selectedIds = if (id in content.selectedIds) {
                content.selectedIds - id
            } else {
                content.selectedIds + id
            }
            content.copy(selectedIds = selectedIds)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val content = state as? EpisodeDownloadUiState.Content ?: return@update state
            if (content.submission is SubmissionState.Submitting) return@update state
            content.copy(selectedIds = content.episodes.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            val content = state as? EpisodeDownloadUiState.Content ?: return@update state
            if (content.submission is SubmissionState.Submitting) return@update state
            content.copy(selectedIds = emptySet())
        }
    }

    fun downloadSelected() {
        val content = _uiState.value as? EpisodeDownloadUiState.Content ?: return
        if (content.submission is SubmissionState.Submitting || content.selectedIds.isEmpty()) return

        val selected = content.episodes.filter { it.id in content.selectedIds }
        if (selected.isEmpty()) return

        _uiState.update { state ->
            val current = state as? EpisodeDownloadUiState.Content ?: return@update state
            current.copy(submission = SubmissionState.Submitting)
        }

        viewModelScope.launch {
            try {
                episodeDownloadManager.enqueue(comicId, comicTitle, coverUrl, selected)
                val successMsg = if (selected.size == 1) "已成功将 1 个章节加入下载队列" else "已成功将 ${selected.size} 个章节加入下载队列"
                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                messageReporter.reportInfo(
                    if (selected.size == 1) {
                        UiText.of(R.string.download_enqueued)
                    } else {
                        UiText.of(R.string.download_episodes_enqueued, selected.size)
                    }
                )
                _uiState.update { state ->
                    val current = state as? EpisodeDownloadUiState.Content ?: return@update state
                    current.copy(submission = SubmissionState.Completed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "下载章节入队失败: comicId=$comicId" }
                Toast.makeText(context, "加入下载队列失败，请稍后重试", Toast.LENGTH_SHORT).show()
                _uiState.update { state ->
                    val current = state as? EpisodeDownloadUiState.Content ?: return@update state
                    current.copy(submission = SubmissionState.Error)
                }
                messageReporter.reportError(
                    UiText.of(R.string.download_enqueue_failed),
                    duration = MessageDuration.Long,
                )
            }
        }
    }

    fun clearCompleted() {
        _uiState.update { state ->
            val content = state as? EpisodeDownloadUiState.Content ?: return@update state
            if (content.submission is SubmissionState.Completed) {
                content.copy(submission = SubmissionState.Idle)
            } else {
                state
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("id") comicId: String,
            @Assisted("title") comicTitle: String,
            @Assisted("url") coverUrl: String,
        ): EpisodeDownloadViewModel
    }
}