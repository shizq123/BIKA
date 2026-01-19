package com.shizq.bika.core.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.CircularProgressIndicator
import kotlinx.coroutines.launch

@Composable
fun Wizard(
    controller: WizardController,
    onFinish: (Map<StepKey, StepResult>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ((stepMetadata: StepMetadata, progress: Float) -> Unit)? = null,
    footer: @Composable ((
        canGoBack: Boolean,
        canGoForward: Boolean,
        validation: ValidationResult,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onCancel: () -> Unit
    ) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    val uiState by controller.uiState.collectAsState()
    val currentStepKey = uiState.currentStepKey
    val stepMap = remember { controller.getAllSteps().associateBy { it.key } }

    // 查找当前步骤
    val currentStep = stepMap[currentStepKey]

    if (currentStep == null) {
        LaunchedEffect(Unit) {
            onCancel()
        }
        return
    }

    // 计算进度
    val progress = remember(uiState.stepKeys, currentStepKey) {
        val currentIndex = uiState.stepKeys.indexOf(currentStepKey)
        if (currentIndex == -1) 0f else (currentIndex + 1) / uiState.stepKeys.size.toFloat()
    }

    // 监听步骤变化，检查是否是最后一步
    LaunchedEffect(currentStepKey, uiState.collectedData) {
        val isLastStep = uiState.stepKeys.lastOrNull() == currentStepKey
        if (isLastStep && currentStep.getNavigationRules(uiState.collectedData).defaultNextStep == null) {
            // 这是最后一步且没有下一步，自动触发完成
            val validation = currentStep.validate()
            if (validation is ValidationResult.Valid) {
                try {
                    val finalData = currentStep.collectData()
                    val allData = uiState.collectedData + (currentStepKey to finalData)
                    onFinish(allData)
                } catch (e: Exception) {
                    // 错误已经在controller中处理
                }
            }
        }
    }

    Column(modifier = modifier) {
        // 头部（可选）
        header?.invoke(currentStep.metadata, progress)

        // 错误显示
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // 步骤内容
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            key(currentStepKey) {
                AnimatedContent(
                    targetState = currentStepKey,
                    transitionSpec = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(300)
                        ) togetherWith slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(300)
                        )
                    },
                    label = "WizardStepTransition"
                ) { stepKey ->
                    val step = stepMap[stepKey]!!

                    step.Content(
                        wizardData = uiState.collectedData,
                        updateValidation = controller::updateValidation,
                        onDataChanged = {
                            // 实时验证可以在这里触发
                            // 例如：launch { controller.validateCurrentStep() }
                        }
                    )
                }
            }

            // 加载指示器
            if (uiState.isNavigating) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 底部导航（可选，如果未提供则使用默认）
        if (footer != null) {
            footer(
                uiState.canGoBack,
                uiState.canGoForward && uiState.currentStepValidation is ValidationResult.Valid,
                uiState.currentStepValidation,
                { scope.launch { controller.next() } },
                { scope.launch { controller.previous() } },
                onCancel
            )
        } else {
            DefaultWizardFooter(
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                validation = uiState.currentStepValidation,
                currentStepMetadata = currentStep.metadata,
                onNext = { scope.launch { controller.next() } },
                onPrevious = { scope.launch { controller.previous() } },
                onCancel = onCancel
            )
        }
    }
}

/**
 * 默认的Wizard底部导航
 */
@Composable
private fun DefaultWizardFooter(
    canGoBack: Boolean,
    canGoForward: Boolean,
    validation: ValidationResult,
    currentStepMetadata: StepMetadata,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 取消按钮
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }

        Row {
            // 上一步按钮
            Button(
                onClick = onPrevious,
                enabled = canGoBack,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Previous")
            }

            // 下一步/完成按钮
            val isNextEnabled = canGoForward && validation is ValidationResult.Valid
            val buttonText = if (currentStepMetadata.isOptional) "Skip" else "Next"

            Button(
                onClick = onNext,
                enabled = isNextEnabled
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun rememberWizardController(
    steps: List<WizardStep<*>>,
    initialStepKey: StepKey? = null
): WizardController {
    return remember(steps, initialStepKey) {
        WizardController.create(steps, initialStepKey)
    }
}