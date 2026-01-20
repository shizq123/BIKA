package com.shizq.bika.core.ui.wizard

/**
 * 校验结果密封类
 */
sealed interface ValidationResult {
    /** 校验成功 */
    object Success : ValidationResult

    /** 校验失败 */
    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : ValidationResult

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
