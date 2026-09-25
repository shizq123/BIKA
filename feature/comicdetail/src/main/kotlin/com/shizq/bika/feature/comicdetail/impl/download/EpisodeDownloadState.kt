package com.shizq.bika.feature.comicdetail.impl.download

import com.shizq.bika.core.data.model.Chapter

sealed interface EpisodeDownloadUiState {
    data object Initial : EpisodeDownloadUiState
    data object Loading : EpisodeDownloadUiState
    data object Empty : EpisodeDownloadUiState
    data object LoadError : EpisodeDownloadUiState

    data class Content(
        val episodes: List<Chapter>,
        val selectedIds: Set<String> = emptySet(),
        val submission: SubmissionState = SubmissionState.Idle,
    ) : EpisodeDownloadUiState
}

sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data object Completed : SubmissionState
    data object Error : SubmissionState
}