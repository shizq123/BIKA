package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.R

@Composable
fun TagBlockDialog(
    tag: String,
    onDismiss: () -> Unit,
    viewModel: TagBlockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) {
            onDismiss()
            viewModel.clearCompleted()
        }
    }

    TagBlockDialogContent(
        tag = tag,
        isSubmitting = uiState.isSubmitting,
        error = uiState.error,
        onDismiss = onDismiss,
        onConfirm = { viewModel.confirm(tag) },
    )
}

@Composable
private fun TagBlockDialogContent(
    tag: String,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(R.string.tag_block_title)) },
        text = {
            Column {
                Text(stringResource(R.string.tag_block_message, tag))
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isSubmitting) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSubmitting, onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}