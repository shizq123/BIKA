package com.shizq.bika.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.ValidationResult

// 安全问题数据类
data class SecurityQuestion(
    val question: String = "",
    val answer: String = ""
)

data class SecurityQuestionsData(
    val questions: List<SecurityQuestion> = List(3) { SecurityQuestion() }
)

// 安全问题状态管理
class SecurityQuestionsState {
    var question1 by mutableStateOf("")
    var answer1 by mutableStateOf("")

    var question2 by mutableStateOf("")
    var answer2 by mutableStateOf("")

    var question3 by mutableStateOf("")
    var answer3 by mutableStateOf("")

    fun toData() = SecurityQuestionsData(
        questions = listOf(
            SecurityQuestion(question1, answer1),
            SecurityQuestion(question2, answer2),
            SecurityQuestion(question3, answer3)
        )
    )

    fun validate(): ValidationResult {
        // 验证第一个问题
        if (question1.isEmpty()) {
            return ValidationResult.Error("请输入第1个安全问题")
        }
        if (answer1.isEmpty()) {
            return ValidationResult.Error("请输入第1个安全问题的答案")
        }

        // 验证第二个问题
        if (question2.isEmpty()) {
            return ValidationResult.Error("请输入第2个安全问题")
        }
        if (answer2.isEmpty()) {
            return ValidationResult.Error("请输入第2个安全问题的答案")
        }

        // 验证第三个问题
        if (question3.isEmpty()) {
            return ValidationResult.Error("请输入第3个安全问题")
        }
        if (answer3.isEmpty()) {
            return ValidationResult.Error("请输入第3个安全问题的答案")
        }

        return ValidationResult.Success
    }
}

// 安全问题输入组件
@Composable
fun SecurityQuestionsStep(
    state: SecurityQuestionsState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 第一个安全问题
        SecurityQuestionItem(
            questionNumber = 1,
            question = state.question1,
            onQuestionChange = { state.question1 = it },
            answer = state.answer1,
            onAnswerChange = { state.answer1 = it },
            isError = errorMessage != null &&
                    (state.question1.isEmpty() || state.answer1.isEmpty())
        )

        HorizontalDivider()

        // 第二个安全问题
        SecurityQuestionItem(
            questionNumber = 2,
            question = state.question2,
            onQuestionChange = { state.question2 = it },
            answer = state.answer2,
            onAnswerChange = { state.answer2 = it },
            isError = errorMessage != null &&
                    (state.question2.isEmpty() || state.answer2.isEmpty())
        )

        HorizontalDivider()

        // 第三个安全问题
        SecurityQuestionItem(
            questionNumber = 3,
            question = state.question3,
            onQuestionChange = { state.question3 = it },
            answer = state.answer3,
            onAnswerChange = { state.answer3 = it },
            isError = errorMessage != null &&
                    (state.question3.isEmpty() || state.answer3.isEmpty())
        )
    }
}

// 单个安全问题项
@Composable
fun SecurityQuestionItem(
    questionNumber: Int,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "安全问题 $questionNumber",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            label = { Text("请输入一个安全问题") },
            placeholder = { Text("例如：您的宠物叫什么名字？") },
            isError = isError && question.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text("请输入答案") },
            isError = isError && answer.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}