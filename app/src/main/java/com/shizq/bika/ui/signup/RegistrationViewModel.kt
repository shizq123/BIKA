package com.shizq.bika.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
        viewModelScope.launch(NonCancellable) {
            registrationState.update { RegistrationUiState.Loading }

            val basicInfoData = basicInfoState.toData()
            val securityData = securityQuestionsState.toData()
            val personalData = personalInfoState.toData()
            val obj = buildJsonObject {
                put("email", JsonPrimitive(basicInfoData.email))
                put("name", JsonPrimitive(basicInfoData.nickname))
                put("password", JsonPrimitive(basicInfoData.password))

                put("birthday", JsonPrimitive(personalData.birthday))
                put("gender", JsonPrimitive(personalData.gender))

                securityData.questions.forEachIndexed { index, question ->
                    put("answer${index + 1}", JsonPrimitive(question.answer))
                    put("question${index + 1}", JsonPrimitive(question.question))
                }
            }
            try {
                val data = api.requestSignUp(obj)
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