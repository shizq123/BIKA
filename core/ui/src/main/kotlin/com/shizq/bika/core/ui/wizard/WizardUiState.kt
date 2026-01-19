package com.shizq.bika.core.ui.wizard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Wizard的UI状态
 */
data class WizardUiState(
    val currentStepKey: StepKey,
    val stepKeys: List<StepKey>,
    val collectedData: Map<StepKey, StepResult>,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val isNavigating: Boolean,
    val currentStepValidation: ValidationResult,
    val error: String? = null
)

/**
 * Wizard控制器，管理状态和导航逻辑
 */
class WizardController private constructor(
    private val steps: List<WizardStep<*>>,
    initialStepKey: StepKey? = null
) {
    private val stepMap = steps.associateBy { it.key }

    private val _uiState = MutableStateFlow(
        WizardUiState(
            currentStepKey = initialStepKey ?: steps.first().key,
            stepKeys = steps.map { it.key },
            collectedData = emptyMap(),
            canGoBack = false,
            canGoForward = true,
            isNavigating = false,
            currentStepValidation = ValidationResult.Valid
        )
    )
    val uiState = _uiState.asStateFlow()

    private val backStack = ArrayDeque<StepKey>()

    /**
     * 获取所有步骤定义
     */
    fun getAllSteps(): List<WizardStep<*>> = steps

    /**
     * 获取当前步骤
     */
    fun getCurrentStep(): WizardStep<*>? = stepMap[_uiState.value.currentStepKey]

    /**
     * 根据Key获取步骤
     */
    fun getStepByKey(key: StepKey): WizardStep<*>? = stepMap[key]

    /**
     * 导航到下一步
     */
    suspend fun next() {
        val currentState = _uiState.value
        val currentStep = stepMap[currentState.currentStepKey] ?: return

        _uiState.update { it.copy(isNavigating = true) }
        val validation = currentStep.validate()

        if (validation is ValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    isNavigating = false,
                    error = validation.message ?: "Validation failed"
                )
            }
            return
        }

        try {
            val data = currentStep.collectData()
            val newData = currentState.collectedData + (currentStep.key to data)

            val rules = currentStep.getNavigationRules(newData)
            val nextStepKey = rules.defaultNextStep ?: findNextAvailableStep(currentStep.key, rules)

            if (nextStepKey != null) {
                backStack.addLast(currentState.currentStepKey)

                _uiState.update {
                    it.copy(
                        currentStepKey = nextStepKey,
                        collectedData = newData,
                        canGoBack = true,
                        canGoForward = true,
                        isNavigating = false,
                        error = null
                    )
                }

                currentStep.onStepHidden()
                stepMap[nextStepKey]?.onStepShown()
            } else {
                _uiState.update {
                    it.copy(
                        isNavigating = false,
                        error = "No next step available"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isNavigating = false,
                    error = "Failed to collect data: ${e.message}"
                )
            }
        }
    }

    /**
     * 导航到上一步
     */
    suspend fun previous() {
        if (backStack.isEmpty()) return

        val currentState = _uiState.value
        val currentStep = stepMap[currentState.currentStepKey]
        val previousStepKey = backStack.removeLast()
        val previousStep = stepMap[previousStepKey]

        if (currentStep == null || previousStep == null) return

        _uiState.update { it.copy(isNavigating = true) }

        try {
            // 1. 通知当前步骤将被隐藏
            currentStep.onStepHidden()

            // 2. 更新UI状态
            _uiState.update {
                it.copy(
                    currentStepKey = previousStepKey,
                    canGoBack = backStack.isNotEmpty(),
                    canGoForward = true,
                    isNavigating = false,
                    error = null
                )
            }

            // 3. 通知新步骤被显示
            previousStep.onStepShown()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isNavigating = false,
                    error = "Failed to navigate back: ${e.message}"
                )
            }
        }
    }

    /**
     * 跳转到指定步骤
     */
    suspend fun goTo(stepKey: StepKey) {
        val currentState = _uiState.value
        val currentStep = stepMap[currentState.currentStepKey] ?: return
        val targetStep = stepMap[stepKey] ?: return

        val rules = currentStep.getNavigationRules(currentState.collectedData)
        if (!rules.canNavigateTo.contains(stepKey)) {
            _uiState.update {
                it.copy(error = "Cannot navigate to step $stepKey from current step")
            }
            return
        }

        if (currentStep.key != stepKey) {
            val validation = currentStep.validate()
            if (validation is ValidationResult.Invalid) {
                _uiState.update {
                    it.copy(error = validation.message ?: "Cannot navigate: validation failed")
                }
                return
            }

            try {
                val data = currentStep.collectData()
                val newData = currentState.collectedData + (currentStep.key to data)

                if (!backStack.contains(currentStep.key) && currentStep.key != backStack.lastOrNull()) {
                    backStack.addLast(currentStep.key)
                }

                _uiState.update {
                    it.copy(
                        currentStepKey = stepKey,
                        collectedData = newData,
                        canGoBack = backStack.isNotEmpty(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to collect data: ${e.message}")
                }
                return
            }
        }

        currentStep.onStepHidden()
        targetStep.onStepShown()
    }

    /**
     * 获取所有收集的数据
     */
    fun getAllData(): Map<StepKey, StepResult> = _uiState.value.collectedData

    /**
     * 更新当前步骤的验证状态（由UI调用）
     */
    fun updateValidation(validation: ValidationResult) {
        _uiState.update { it.copy(currentStepValidation = validation) }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun findNextAvailableStep(
        currentStepKey: StepKey,
        rules: NavigationRules
    ): StepKey? {
        val currentIndex = steps.indexOfFirst { it.key == currentStepKey }
        if (currentIndex == -1) return null

        // 首先检查默认下一步
        rules.defaultNextStep?.let { defaultNext ->
            if (rules.canNavigateTo.isEmpty() || rules.canNavigateTo.contains(defaultNext)) {
                return defaultNext
            }
        }

        // 如果没有默认，检查是否可以导航到后续步骤
        for (i in currentIndex + 1 until steps.size) {
            val candidateKey = steps[i].key
            if (rules.canNavigateTo.isEmpty() || rules.canNavigateTo.contains(candidateKey)) {
                return candidateKey
            }
        }

        return null
    }

    companion object {
        /**
         * 创建WizardController
         */
        fun create(
            steps: List<WizardStep<*>>,
            initialStepKey: StepKey? = null
        ): WizardController {
            require(steps.isNotEmpty()) { "Wizard must have at least one step" }
            require(steps.map { it.key }.distinct().size == steps.size) {
                "Step keys must be unique"
            }

            val actualInitialStep = initialStepKey ?: steps.first().key
            require(steps.any { it.key == actualInitialStep }) {
                "Initial step key not found in steps"
            }

            return WizardController(steps, actualInitialStep)
        }
    }
}