package com.shizq.bika.ui.comicinfo.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.R
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportError
import com.shizq.bika.core.message.reportInfo
import com.shizq.bika.ui.comicinfo.ComicDownloadEnqueuer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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
    @Assisted private val comicId: String,
    @Assisted private val comicTitle: String,
    @Assisted private val coverUrl: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EpisodeDownloadUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        val state = _uiState.value
        if (state.isLoading || state.hasLoaded) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val episodes = chapterRepository.getAllChapters(comicId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = true,
                        episodes = episodes,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "获取可选章节失败: comicId=$comicId" }
                _uiState.update {
                    it.copy(isLoading = false, error = "获取章节失败，请重试")
                }
                messageReporter.reportError(
                    UiText.of(R.string.download_fetch_chapters_failed),
                    duration = MessageDuration.Long,
                )
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(hasLoaded = false, error = null) }
        load()
    }

    fun toggleEpisode(id: String) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { state ->
            val selected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = selected)
        }
    }

    fun selectAll() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { state -> state.copy(selectedIds = state.episodes.map { it.id }.toSet()) }
    }

    fun clearSelection() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun downloadSelected() {
        val state = _uiState.value
        if (state.isSubmitting || state.selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val selected = state.episodes.filter { it.id in state.selectedIds }
                episodeDownloadManager.enqueue(comicId, comicTitle, coverUrl, selected)
                messageReporter.reportInfo(
                    if (selected.size == 1) {
                        UiText.of(R.string.download_enqueued)
                    } else {
                        UiText.of(R.string.download_episodes_enqueued, selected.size)
                    }
                )
                _uiState.update { it.copy(isSubmitting = false, completed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "下载章节入队失败: comicId=$comicId" }
                _uiState.update {
                    it.copy(isSubmitting = false, error = "下载失败，请重试")
                }
                messageReporter.reportError(UiText.of(R.string.download_fetch_chapters_failed))
            }
        }
    }

    fun clearCompleted() {
        _uiState.update { it.copy(completed = false) }
    }

    @AssistedFactory
    interface Factory {
        fun create(comicId: String, comicTitle: String, coverUrl: String): EpisodeDownloadViewModel
    }
}

data class EpisodeDownloadUiState(
    val episodes: List<Chapter> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasLoaded: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null,
)
