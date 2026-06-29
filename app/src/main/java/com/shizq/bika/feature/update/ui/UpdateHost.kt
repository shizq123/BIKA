package com.shizq.bika.feature.update.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UpdateHost(
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(
            UpdateAction.CheckUpdate(
                source = UpdateCheckSource.Auto,
            ),
        )
    }

    UpdateEffectHandler(
        effects = viewModel.effects,
        onError = { message ->
            viewModel.dispatch(UpdateAction.ShowError(message))
        },
    )

    UpdateDialog(
        state = state,
        onDismiss = {
            when (val currentState = state) {
                is UpdateUiState.HasUpdate -> {
                    if (!currentState.release.forceUpdate) {
                        viewModel.dispatch(
                            UpdateAction.RemindLater(currentState.release),
                        )
                    }
                }

                else -> {
                    viewModel.dispatch(UpdateAction.Reset)
                }
            }
        },
        onStartDownload = { release ->
            viewModel.dispatch(
                UpdateAction.StartDownload(release),
            )
        },
        onIgnoreVersion = { release ->
            viewModel.dispatch(
                UpdateAction.IgnoreVersion(release),
            )
        },
        onRetry = { retryAction ->
            viewModel.dispatch(
                UpdateAction.Retry(retryAction),
            )
        },
    )
}