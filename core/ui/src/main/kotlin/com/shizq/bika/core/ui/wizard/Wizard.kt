package com.shizq.bika.core.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shizq.bika.core.ui.wizard.components.DefaultWizardControls
import com.shizq.bika.core.ui.wizard.components.DefaultWizardIndicator

/**
 * 向导主组件
 */
@Composable
fun <T : WizardStepData> Wizard(
    state: WizardState<T>,
    modifier: Modifier = Modifier,
    onFinish: (Map<String, T>) -> Unit,
    onCancel: (() -> Unit)? = null,
    colors: WizardColors = WizardDefaults.colors(),
    indicator: @Composable (WizardState<T>) -> Unit = {
        DefaultWizardIndicator(it, colors)
    },
    controls: @Composable (WizardState<T>, onFinish: () -> Unit, onCancel: () -> Unit) -> Unit = { s, finish, cancel ->
        DefaultWizardControls(s, finish, cancel, colors = colors)
    },
    contentTransition: WizardContentTransition = WizardDefaults.contentTransition()
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 步骤指示器
        indicator(state)

        // 步骤内容
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = state.currentStepIndex,
                transitionSpec = {
                    contentTransition.createTransitionSpec(state.navigationDirection).invoke(this)
                },
                label = "WizardContent"
            ) { stepIndex ->
                val stepConfig = state.steps[stepIndex]

                @Suppress("UNCHECKED_CAST")
                val content = stepConfig.content as @Composable (
                    T, ValidationResult.Error?, (T) -> Unit
                ) -> Unit

                content(
                    state.getCurrentStepData(),
                    state.currentValidationError,
                    state::updateCurrentStepData
                )
            }
        }

        // 导航控件
        controls(
            state,
            { state.finish(onFinish) },
            { onCancel?.let { state.cancel(it) } }
        )
    }
}
