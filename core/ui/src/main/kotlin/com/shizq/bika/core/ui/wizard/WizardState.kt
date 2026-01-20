package com.shizq.bika.core.ui.wizard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 创建并记忆 WizardState
 */
@Composable
fun <T : WizardStepData> rememberWizardState(
    steps: List<WizardStepConfig<out T>>,
    onStepChanged: ((fromIndex: Int, toIndex: Int) -> Unit)? = null
): WizardState<T> {
    val coroutineScope = rememberCoroutineScope()

    return remember(steps) {
        WizardState(
            steps = steps,
            coroutineScope = coroutineScope,
            onStepChanged = onStepChanged
        )
    }
}

/**
 * 创建可保存的 WizardState（支持配置变更恢复）
 */
@Composable
fun <T : WizardStepData> rememberSaveableWizardState(
    steps: List<WizardStepConfig<out T>>,
    onStepChanged: ((fromIndex: Int, toIndex: Int) -> Unit)? = null
): WizardState<T> {
    // 实现 Saver 以支持状态保存
    // 注意：步骤数据需要实现 Parcelable 或提供自定义序列化
    TODO("实现状态保存逻辑")
}

/**
 * 向导状态管理器
 *
 * @param T 步骤数据的基类型（通常是密封接口）
 */
@Stable
class WizardState<T : WizardStepData> internal constructor(
    val steps: List<WizardStepConfig<out T>>,
    private val coroutineScope: CoroutineScope,
    private val onStepChanged: ((fromIndex: Int, toIndex: Int) -> Unit)? = null
) {
    // ==================== 基础状态 ====================

    /** 当前步骤索引 */
    var currentStepIndex: Int by mutableIntStateOf(0)
        private set

    /** 导航方向 */
    var navigationDirection: NavigationDirection by mutableStateOf(NavigationDirection.NONE)
        private set

    /** 向导状态 */
    var status: WizardStatus by mutableStateOf(WizardStatus.IN_PROGRESS)
        private set

    /** 当前校验错误 */
    var currentValidationError: ValidationResult.Error? by mutableStateOf(null)
        private set

    // ==================== 派生状态 ====================

    /** 步骤总数 */
    val totalSteps: Int get() = steps.size

    /** 当前步骤配置 */
    val currentStepConfig: WizardStepConfig<out T>
        get() = steps[currentStepIndex]

    /** 是否第一步 */
    val isFirstStep: Boolean by derivedStateOf { currentStepIndex == 0 }

    /** 是否最后一步 */
    val isLastStep: Boolean by derivedStateOf { currentStepIndex == steps.lastIndex }

    /** 是否加载中 */
    val isLoading: Boolean by derivedStateOf { status == WizardStatus.LOADING }

    /** 进度百分比 (0.0 - 1.0) */
    val progress: Float by derivedStateOf {
        (currentStepIndex + 1).toFloat() / totalSteps
    }

    // ==================== 数据存储 ====================

    /** 各步骤数据存储 */
    private val _stepDataMap = mutableStateMapOf<String, T>().apply {
        steps.forEach { step ->
            @Suppress("UNCHECKED_CAST")
            put(step.id, step.initialData as T)
        }
    }

    /** 已完成的步骤 */
    private val _completedSteps = mutableStateSetOf<String>()
    val completedSteps: Set<String> get() = _completedSteps

    // ==================== 数据访问 ====================

    /**
     * 获取指定步骤的数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <D : T> getStepData(stepId: String): D? = _stepDataMap[stepId] as? D

    /**
     * 获取当前步骤的数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <D : T> getCurrentStepData(): D = _stepDataMap[currentStepConfig.id] as D

    /**
     * 获取所有步骤数据
     */
    fun getAllData(): Map<String, T> = _stepDataMap.toMap()

    /**
     * 更新当前步骤数据
     */
    fun updateCurrentStepData(data: T) {
        _stepDataMap[currentStepConfig.id] = data
        // 数据变化时清除校验错误
        currentValidationError = null
    }

    // ==================== 导航操作 ====================

    /**
     * 前进到下一步
     * @return 是否成功前进
     */
    fun next(): Boolean {
        if (isLastStep || isLoading) return false

        val currentData = getCurrentStepData<T>()

        // 同步校验
        val syncResult = currentData.validate()
        if (syncResult is ValidationResult.Error) {
            currentValidationError = syncResult
            return false
        }

        // 异步校验
        val asyncValidator = currentStepConfig.asyncValidator
        if (asyncValidator != null) {
            coroutineScope.launch {
                status = WizardStatus.LOADING
                try {
                    @Suppress("UNCHECKED_CAST")
                    val asyncResult =
                        (asyncValidator as suspend (T) -> ValidationResult)(currentData)
                    if (asyncResult is ValidationResult.Error) {
                        currentValidationError = asyncResult
                    } else {
                        performNavigation(currentStepIndex + 1, NavigationDirection.FORWARD)
                    }
                } finally {
                    status = WizardStatus.IN_PROGRESS
                }
            }
            return true
        }

        performNavigation(currentStepIndex + 1, NavigationDirection.FORWARD)
        return true
    }

    /**
     * 返回上一步
     * @return 是否成功返回
     */
    fun back(): Boolean {
        if (isFirstStep || isLoading) return false
        performNavigation(currentStepIndex - 1, NavigationDirection.BACKWARD)
        return true
    }

    /**
     * 跳转到指定步骤
     * @param stepIndex 目标步骤索引
     * @param skipValidation 是否跳过校验（仅向后跳转时有效）
     */
    fun goToStep(stepIndex: Int, skipValidation: Boolean = false): Boolean {
        if (stepIndex < 0 || stepIndex >= totalSteps || isLoading) return false
        if (stepIndex == currentStepIndex) return true

        // 向前跳转需要校验当前步骤（除非跳过）
        if (stepIndex > currentStepIndex && !skipValidation) {
            val currentData = getCurrentStepData<T>()
            val result = currentData.validate()
            if (result is ValidationResult.Error) {
                currentValidationError = result
                return false
            }
        }

        val direction = if (stepIndex > currentStepIndex) {
            NavigationDirection.FORWARD
        } else {
            NavigationDirection.BACKWARD
        }

        performNavigation(stepIndex, direction)
        return true
    }

    /**
     * 完成向导
     * @param onComplete 完成回调，接收所有数据
     */
    fun finish(onComplete: (Map<String, T>) -> Unit) {
        if (!isLastStep || isLoading) return

        val currentData = getCurrentStepData<T>()
        val result = currentData.validate()

        if (result is ValidationResult.Error) {
            currentValidationError = result
            return
        }

        _completedSteps.add(currentStepConfig.id)
        status = WizardStatus.COMPLETED
        onComplete(getAllData())
    }

    /**
     * 取消向导
     */
    fun cancel(onCancel: () -> Unit) {
        status = WizardStatus.CANCELLED
        onCancel()
    }

    /**
     * 重置向导
     */
    fun reset() {
        currentStepIndex = 0
        navigationDirection = NavigationDirection.NONE
        status = WizardStatus.IN_PROGRESS
        currentValidationError = null
        _completedSteps.clear()
        _stepDataMap.clear()
        steps.forEach { step ->
            @Suppress("UNCHECKED_CAST")
            _stepDataMap[step.id] = step.initialData as T
        }
    }

    // ==================== 内部方法 ====================

    private fun performNavigation(targetIndex: Int, direction: NavigationDirection) {
        val fromIndex = currentStepIndex
        _completedSteps.add(currentStepConfig.id)
        currentValidationError = null
        navigationDirection = direction
        currentStepIndex = targetIndex
        onStepChanged?.invoke(fromIndex, targetIndex)
    }

    /**
     * 检查步骤是否已完成
     */
    fun isStepCompleted(stepIndex: Int): Boolean {
        return steps.getOrNull(stepIndex)?.id in _completedSteps
    }

    /**
     * 获取步骤状态
     */
    fun getStepStatus(stepIndex: Int): StepStatus {
        return when {
            stepIndex < currentStepIndex -> StepStatus.COMPLETED
            stepIndex == currentStepIndex -> StepStatus.CURRENT
            else -> StepStatus.UPCOMING
        }
    }
}

/**
 * 步骤状态
 */
enum class StepStatus {
    COMPLETED,
    CURRENT,
    UPCOMING
}

/**
 * 导航方向，用于动画控制
 */
enum class NavigationDirection {
    FORWARD,
    BACKWARD,
    NONE
}

/**
 * 向导运行状态
 */
enum class WizardStatus {
    /** 进行中 */
    IN_PROGRESS,

    /** 加载中（异步校验/提交） */
    LOADING,

    /** 已完成 */
    COMPLETED,

    /** 已取消 */
    CANCELLED
}