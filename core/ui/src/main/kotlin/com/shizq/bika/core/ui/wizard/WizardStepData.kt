package com.shizq.bika.core.ui.wizard

/**
 * 所有步骤数据的基类
 * 使用密封接口确保类型安全和穷尽性检查
 */
sealed interface WizardStepData {
    /**
     * 校验当前数据是否有效
     * @return 校验结果
     */
    fun validate(): ValidationResult = ValidationResult.Success
}
