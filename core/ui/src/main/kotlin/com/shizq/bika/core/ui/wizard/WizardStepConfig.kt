package com.shizq.bika.core.ui.wizard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 向导步骤配置
 * @param T 该步骤对应的数据类型
 */
data class WizardStepConfig<T : WizardStepData>(
    /** 步骤唯一标识 */
    val id: String,

    /** 步骤标题 */
    val title: String,

    /** 步骤副标题/描述（可选） */
    val subtitle: String? = null,

    /** 步骤图标（可选） */
    val icon: ImageVector? = null,

    /** 初始数据 */
    val initialData: T,

    /** 是否可选步骤（可跳过） */
    val isOptional: Boolean = false,

    /** 异步校验器（可选，用于需要网络请求的校验） */
    val asyncValidator: (suspend (T) -> ValidationResult)? = null,

    /** 步骤内容 */
    val content: @Composable (
        data: T,
        validationError: ValidationResult.Error?,
        onDataChanged: (T) -> Unit
    ) -> Unit
)
