package com.shizq.bika.core.ui.wizard

import androidx.compose.runtime.Composable

data class WizardStep(
    val title: String,
    val content: @Composable (errorMessage: String?) -> Unit,
    val validate: () -> ValidationResult = { ValidationResult.Success }
)

sealed interface ValidationResult {
    data object Success : ValidationResult
    data class Error(val message: String) : ValidationResult
}