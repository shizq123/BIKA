package com.shizq.bika.core.ui.wizard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 用于简化创建步骤的抽象基类
 */
abstract class AbstractWizardStep<T : StepResult>(
    override val key: StepKey,
    override val metadata: StepMetadata
) : WizardStep<T> {

    protected var currentValidation: ValidationResult = ValidationResult.Valid

    @Composable
    override fun Content(
        wizardData: Map<StepKey, StepResult>,
        updateValidation: (ValidationResult) -> Unit,
        onDataChanged: (() -> Unit)?
    ) {
        LaunchedEffect(key1 = currentValidation) {
            updateValidation(currentValidation)
        }

        ContentInternal(wizardData, onDataChanged)
    }

    @Composable
    protected abstract fun ContentInternal(
        wizardData: Map<StepKey, StepResult>,
        onDataChanged: (() -> Unit)?
    )

    protected fun updateValidation(result: ValidationResult) {
        currentValidation = result
    }
}