package com.shizq.bika.feature.settings.impl.update.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UpdateHost(
    autoCheckOnLaunch: Boolean = true,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (autoCheckOnLaunch) {
        LaunchedEffect(Unit) {
            viewModel.dispatch(
                UpdateAction.CheckUpdate(
                    source = UpdateCheckSource.Auto,
                ),
            )
        }
    }

    // 手动检查（用户主动点击"检查更新"）时，NoUpdate 需要明确反馈，Auto 静默检查则不打断用户
    LaunchedEffect(state) {
        val currentState = state
        if (currentState is UpdateUiState.NoUpdate && currentState.source == UpdateCheckSource.Manual) {
            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
            viewModel.dispatch(UpdateAction.Reset)
        }
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