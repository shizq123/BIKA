package com.shizq.bika.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val api: BikaDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
) : ViewModel() {
    val registrationState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.None)
    val basicInfoState = BasicInfoState()
    val securityQuestionsState = SecurityQuestionsState()
    val personalInfoState = PersonalInfoState()

    fun registerUser() {
        viewModelScope.launch {
            registrationState.update { RegistrationUiState.Loading }

            val basicInfoData = basicInfoState.toData()
            val securityData = securityQuestionsState.toData()
            val personalData = personalInfoState.toData()
            val questions = securityData.questions
            try {
                val data = api.signUp(
                    email = basicInfoData.email,
                    password = basicInfoData.password,
                    name = basicInfoData.nickname,
                    birthday = personalData.birthday,
                    gender = personalData.gender,
                    question1 = questions[0].question,
                    answer1 = questions[0].answer,
                    question2 = questions[1].question,
                    answer2 = questions[1].answer,
                    question3 = questions[2].question,
                    answer3 = questions[2].answer,
                )
                val code = data["code"]?.jsonPrimitive?.int
                if (code == 200) {
                    userCredentialsDataSource.setUsername(basicInfoData.email)
                    userCredentialsDataSource.setPassword(basicInfoData.password)
                    registrationState.update { RegistrationUiState.Success }
                } else {
                    val msg = data["message"]?.jsonPrimitive?.content
                    registrationState.update { RegistrationUiState.Error(msg.toString()) }
                }
            } catch (e: Exception) {
                registrationState.update { RegistrationUiState.Error(e.message.toString()) }
            }
        }
    }
}