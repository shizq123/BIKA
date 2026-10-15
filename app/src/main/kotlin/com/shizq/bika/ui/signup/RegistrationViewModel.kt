package com.shizq.bika.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
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

            // 1. 前置守卫校验：安全问题数量与内容非空防御
            if (questions.size < 3 || questions.take(3).any { it.question.isBlank() || it.answer.isBlank() }) {
                registrationState.update { RegistrationUiState.Error("请完整设置 3 个安全问题及答案") }
                return@launch
            }

            // 2. 防御性安全提取，绝不触发 IndexOutOfBoundsException
            val q1 = questions.getOrNull(0)
            val q2 = questions.getOrNull(1)
            val q3 = questions.getOrNull(2)

            try {
                val data = api.signUp(
                    email = basicInfoData.email,
                    password = basicInfoData.password,
                    name = basicInfoData.nickname,
                    birthday = personalData.birthday,
                    gender = personalData.gender,
                    question1 = q1?.question.orEmpty(),
                    answer1 = q1?.answer.orEmpty(),
                    question2 = q2?.question.orEmpty(),
                    answer2 = q2?.answer.orEmpty(),
                    question3 = q3?.question.orEmpty(),
                    answer3 = q3?.answer.orEmpty(),
                )
                val code = data["code"]?.jsonPrimitive?.int
                if (code == 200) {
                    userCredentialsDataSource.setUsername(basicInfoData.email)
                    userCredentialsDataSource.setPassword(basicInfoData.password)
                    registrationState.update { RegistrationUiState.Success }
                } else {
                    val msg = data["message"]?.jsonPrimitive?.content ?: "注册失败"
                    registrationState.update { RegistrationUiState.Error(msg) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                registrationState.update { RegistrationUiState.Error(e.message ?: "未知网络异常") }
            }
        }
    }
}