package com.shizq.bika.core.ui.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.StepStatus
import com.shizq.bika.core.ui.wizard.WizardColors
import com.shizq.bika.core.ui.wizard.WizardDefaults
import com.shizq.bika.core.ui.wizard.WizardState
import com.shizq.bika.core.ui.wizard.WizardStepData

/**
 * 点状步骤指示器（可选样式）
 */
@Composable
fun <T : WizardStepData> DotWizardIndicator(
    state: WizardState<T>,
    modifier: Modifier = Modifier,
    colors: WizardColors = WizardDefaults.colors()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(state.totalSteps) { index ->
            val status = state.getStepStatus(index)

            Box(
                modifier = Modifier
                    .size(if (status == StepStatus.CURRENT) 12.dp else 8.dp)
                    .background(
                        color = when (status) {
                            StepStatus.COMPLETED -> colors.completedStepColor
                            StepStatus.CURRENT -> colors.currentStepColor
                            StepStatus.UPCOMING -> colors.upcomingStepColor
                        },
                        shape = CircleShape
                    )
            )

            if (index < state.totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}