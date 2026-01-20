package com.shizq.bika.core.ui.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.WizardColors
import com.shizq.bika.core.ui.wizard.WizardDefaults
import com.shizq.bika.core.ui.wizard.WizardState
import com.shizq.bika.core.ui.wizard.WizardStepData

/**
 * 默认导航控件
 */
@Composable
fun <T : WizardStepData> DefaultWizardControls(
    state: WizardState<T>,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    colors: WizardColors = WizardDefaults.colors(),
    backText: String = "上一步",
    nextText: String = "下一步",
    finishText: String = "完成",
    cancelText: String = "取消"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：取消/上一步
            Row {
                // 取消按钮
                TextButton(
                    onClick = onCancel,
                    enabled = !state.isLoading
                ) {
                    Text(cancelText)
                }

                // 上一步按钮
                if (!state.isFirstStep) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { state.back() },
                        enabled = !state.isLoading
                    ) {
                        Text(backText)
                    }
                }
            }

            // 右侧：下一步/完成
            Button(
                onClick = {
                    if (state.isLastStep) {
                        onFinish()
                    } else {
                        state.next()
                    }
                },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryButtonColor
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (state.isLastStep) finishText else nextText)
            }
        }
    }
}
