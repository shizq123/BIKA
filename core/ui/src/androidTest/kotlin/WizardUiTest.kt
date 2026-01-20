import com.shizq.bika.core.ui.wizard.Wizard
import com.shizq.bika.core.ui.wizard.rememberWizardState
import org.junit.Rule
import kotlin.test.Test

class WizardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun wizard_displays_first_step_initially() {
        composeTestRule.setContent {
            val steps = createTestSteps()
            val state = rememberWizardState(steps = steps)

            Wizard(
                state = state,
                onFinish = {},
                onCancel = {}
            )
        }

        composeTestRule
            .onNodeWithText("步骤 1")
            .assertIsDisplayed()
    }

    @Test
    fun wizard_navigates_to_next_step_on_button_click() {
        composeTestRule.setContent {
            val steps = createTestSteps()
            val state = rememberWizardState(steps = steps)

            Wizard(
                state = state,
                onFinish = {},
                onCancel = {}
            )
        }

        // 填写数据
        composeTestRule
            .onNodeWithTag("name_input")
            .performTextInput("Test Name")

        // 点击下一步
        composeTestRule
            .onNodeWithText("下一步")
            .performClick()

        // 验证显示第二步
        composeTestRule
            .onNodeWithText("步骤 2")
            .assertIsDisplayed()
    }

    @Test
    fun wizard_shows_validation_error() {
        composeTestRule.setContent {
            val steps = createTestSteps()
            val state = rememberWizardState(steps = steps)

            Wizard(
                state = state,
                onFinish = {},
                onCancel = {}
            )
        }

        // 不填写数据直接点击下一步
        composeTestRule
            .onNodeWithText("下一步")
            .performClick()

        // 验证显示错误信息
        composeTestRule
            .onNodeWithText("姓名不能为空")
            .assertIsDisplayed()
    }
}
