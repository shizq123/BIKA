package com.shizq.bika.core.ui.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.WizardColors
import com.shizq.bika.core.ui.wizard.WizardDefaults
import com.shizq.bika.core.ui.wizard.WizardState
import com.shizq.bika.core.ui.wizard.WizardStepData

/**
 * 默认步骤指示器
 */
@Composable
fun <T : WizardStepData> DefaultWizardIndicator(
    state: WizardState<T>,
    colors: WizardColors = WizardDefaults.colors(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // 进度条
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
            color = colors.progressColor,
            trackColor = colors.progressTrackColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 步骤信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.currentStepConfig.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.titleColor
            )

            Text(
                text = "${state.currentStepIndex + 1} / ${state.totalSteps}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.subtitleColor
            )
        }

        state.currentStepConfig.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtitleColor
            )
        }
    }
}
