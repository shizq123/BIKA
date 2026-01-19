package com.shizq.bika.core.ui.wizard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Wizard步骤的基类接口
 * @param T 该步骤产生的数据类型
 */
interface WizardStep<T : StepResult> {
    /**
     * 步骤的唯一标识符
     */
    val key: StepKey

    /**
     * 步骤的元数据
     */
    val metadata: StepMetadata

    /**
     * 渲染步骤内容
     * @param wizardData 已收集的所有步骤数据
     * @param updateValidation 更新验证状态的回调
     * @param onDataChanged 数据变更时的回调（可选，用于实时验证）
     */
    @Composable
    fun Content(
        wizardData: Map<StepKey, StepResult>,
        updateValidation: (ValidationResult) -> Unit,
        onDataChanged: (() -> Unit)? = null
    )

    /**
     * 验证步骤数据
     * 可以是同步或异步的
     */
    suspend fun validate(): ValidationResult

    /**
     * 收集步骤数据（仅在验证通过后调用）
     */
    suspend fun collectData(): T

    /**
     * 获取导航规则
     * @param wizardData 已收集的所有步骤数据，用于动态决定导航规则
     */
    fun getNavigationRules(wizardData: Map<StepKey, StepResult>): NavigationRules

    /**
     * 步骤被显示时的回调
     */
    fun onStepShown() = Unit

    /**
     * 步骤被隐藏时的回调
     */
    fun onStepHidden() = Unit
}

/**
 * 步骤的唯一标识符
 */
@JvmInline
value class StepKey(val value: String)

/**
 * 步骤产生的结果数据
 */
sealed interface StepResult

/**
 * 步骤的导航规则
 */
data class NavigationRules(
    val canNavigateTo: Set<StepKey> = emptySet(),
    val defaultNextStep: StepKey? = null,
    val canSkip: Boolean = false
)

/**
 * 步骤的元数据
 */
data class StepMetadata(
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val isOptional: Boolean = false
)