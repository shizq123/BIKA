package com.shizq.bika.ui.signup

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.wizard.Wizard
import com.shizq.bika.core.ui.wizard.WizardStep

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel(),
    onBackClicked: () -> Unit
) {
    val registrationUiState by viewModel.registrationState.collectAsStateWithLifecycle()

    RegistrationContent(
        registrationUiState = registrationUiState,
        basicInfoState = viewModel.basicInfoState,
        securityQuestionsState = viewModel.securityQuestionsState,
        personalInfoState = viewModel.personalInfoState,
        onBackClicked = onBackClicked,
        onCompleterClicked = viewModel::registerUser
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationContent(
    registrationUiState: RegistrationUiState,
    basicInfoState: BasicInfoState,
    securityQuestionsState: SecurityQuestionsState,
    personalInfoState: PersonalInfoState,
    onBackClicked: () -> Unit,
    onCompleterClicked: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(registrationUiState) {
        when (registrationUiState) {
            RegistrationUiState.Loading,
            RegistrationUiState.None -> {
            }

            is RegistrationUiState.Error -> Toast.makeText(
                context, registrationUiState.message, Toast.LENGTH_SHORT
            ).show()

            RegistrationUiState.Success -> onBackClicked()
        }
    }

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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("注册") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Wizard(
            steps = steps,
            onComplete = onCompleterClicked,
            modifier = Modifier.padding(innerPadding)
        )
    }
}