package com.shizq.bika.core.ui.wizard

/**
 * 步骤的验证结果
 */
sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val message: String? = null) : ValidationResult
    data class Validating(val message: String? = null) : ValidationResult
}