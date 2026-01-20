package com.shizq.bika.ui.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shizq.bika.core.ui.wizard.Wizard
import com.shizq.bika.core.ui.wizard.WizardStep

@Composable
fun RegistrationScreen() {
    RegistrationContent()
}

@Composable
fun RegistrationContent() {
    WizardExample()
}

@Composable
fun WizardExample() {
    val basicInfoState = remember { BasicInfoState() }
    val securityQuestionsState = remember { SecurityQuestionsState() }
    val personalInfoState = remember { PersonalInfoState() }

    val steps = listOf(
        WizardStep(
            title = "基本信息",
            content = { errorMessage ->
                BasicInfoStep(
                    state = basicInfoState,
                    errorMessage = errorMessage
                )
            },
            validate = {
                basicInfoState.validate()
            }
        ),
        WizardStep(
            title = "安全问题",
            content = { errorMessage ->
                SecurityQuestionsStep(
                    state = securityQuestionsState,
                    errorMessage = errorMessage
                )
            },
            validate = {
                securityQuestionsState.validate()
            },
        ),
        WizardStep(
            title = "个人信息",
            content = { errorMessage ->
                PersonalInfoStep(
                    state = personalInfoState,
                    errorMessage = errorMessage
                )
            },
            validate = {
                personalInfoState.validate()
            }
        ),
    )

    Wizard(
        steps = steps,
        onComplete = {
        }
    )
}