import com.shizq.bika.core.ui.wizard.WizardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WizardStateTest {

    @Test
    fun `initial state should be first step`() {
        val steps = createTestSteps()
        val state = WizardState(
            steps = steps,
            coroutineScope = TestScope()
        )

        assertEquals(0, state.currentStepIndex)
        assertTrue(state.isFirstStep)
        assertFalse(state.isLastStep)
    }

    @Test
    fun `next should advance to next step when validation passes`() {
        val steps = createTestSteps()
        val state = WizardState(
            steps = steps,
            coroutineScope = TestScope()
        )

        // 设置有效数据
        state.updateCurrentStepData(TestStepData.Step1(name = "Test"))

        val result = state.next()

        assertTrue(result)
        assertEquals(1, state.currentStepIndex)
    }

    @Test
    fun `next should not advance when validation fails`() {
        val steps = createTestSteps()
        val state = WizardState(
            steps = steps,
            coroutineScope = TestScope()
        )

        // 保持空数据（校验会失败）
        val result = state.next()

        assertFalse(result)
        assertEquals(0, state.currentStepIndex)
        assertNotNull(state.currentValidationError)
    }

    @Test
    fun `back should return to previous step`() {
        val steps = createTestSteps()
        val state = WizardState(
            steps = steps,
            coroutineScope = TestScope()
        )

        // 先前进
        state.updateCurrentStepData(TestStepData.Step1(name = "Test"))
        state.next()
        assertEquals(1, state.currentStepIndex)

        // 再后退
        state.back()
        assertEquals(0, state.currentStepIndex)
    }

    @Test
    fun `reset should restore initial state`() {
        val steps = createTestSteps()
        val state = WizardState(
            steps = steps,
            coroutineScope = TestScope()
        )

        // 修改状态
        state.updateCurrentStepData(TestStepData.Step1(name = "Test"))
        state.next()

        // 重置
        state.reset()

        assertEquals(0, state.currentStepIndex)
        assertEquals(TestStepData.Step1(), state.getCurrentStepData())
    }
}
