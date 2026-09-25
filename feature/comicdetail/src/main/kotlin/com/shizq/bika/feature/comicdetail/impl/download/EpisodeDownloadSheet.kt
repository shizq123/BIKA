package com.shizq.bika.feature.comicdetail.impl.download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.feature.comicdetail.impl.R

@Composable
fun EpisodeDownloadSheet(
    onDismiss: () -> Unit,
    viewModel: EpisodeDownloadViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState) {
        if (uiState is EpisodeDownloadUiState.Content &&
            (uiState as EpisodeDownloadUiState.Content).submission is SubmissionState.Completed
        ) {
            onDismiss()
            viewModel.clearCompleted()
        }
    }

    EpisodeDownloadSheetContent(
        uiState = uiState,
        onToggleEpisode = viewModel::toggleEpisode,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onRetry = viewModel::retry,
        onDownload = viewModel::downloadSelected,
    )
}

@Composable
fun EpisodeDownloadSheetContent(
    uiState: EpisodeDownloadUiState,
    onToggleEpisode: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.download_select_episodes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val content = uiState as? EpisodeDownloadUiState.Content
            if (content != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = content.submission !is SubmissionState.Submitting,
                        onClick = onSelectAll,
                    ) {
                        Text(stringResource(R.string.download_select_all))
                    }
                    TextButton(
                        enabled = content.submission !is SubmissionState.Submitting,
                        onClick = onClearSelection,
                    ) {
                        Text(stringResource(R.string.download_clear_selection))
                    }
                }
            }
        }

        when (uiState) {
            EpisodeDownloadUiState.Initial,
            EpisodeDownloadUiState.Loading,
                -> LoadingContent()

            EpisodeDownloadUiState.Empty -> EmptyContent()
            EpisodeDownloadUiState.LoadError -> ErrorContent(onRetry = onRetry)
            is EpisodeDownloadUiState.Content -> EpisodeSelectionContent(
                content = uiState,
                modifier = Modifier.weight(1f, fill = false),
                onToggleEpisode = onToggleEpisode,
                onDownload = onDownload,
            )
        }
    }
}

@Composable
private fun EpisodeSelectionContent(
    content: EpisodeDownloadUiState.Content,
    modifier: Modifier = Modifier,
    onToggleEpisode: (String) -> Unit,
    onDownload: () -> Unit,
) {
    val isSubmitting = content.submission is SubmissionState.Submitting

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(content.episodes.size) { index ->
            val episode = content.episodes[index]
            val isSelected = episode.id in content.selectedIds
            Surface(
                onClick = { onToggleEpisode(episode.id) },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    Button(
        onClick = onDownload,
        enabled = content.selectedIds.isNotEmpty() && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator()
        } else {
            Text(
                stringResource(
                    R.string.download_start_selected,
                    content.selectedIds.size,
                )
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.download_no_selectable_episodes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.download_fetch_chapters_failed),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
