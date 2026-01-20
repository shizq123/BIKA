package com.shizq.bika.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.ValidationResult

data class SecurityQuestion(
    val question: String = "",
    val answer: String = ""
)

data class SecurityQuestionsData(
    val questions: List<SecurityQuestion> = List(QUESTION_COUNT) { SecurityQuestion() }
)

class SecurityQuestionsState {
    private val _questions = mutableStateListOf(
        SecurityQuestion(),
        SecurityQuestion(),
        SecurityQuestion()
    )

    fun getQuestion(index: Int): SecurityQuestion = _questions[index]

    fun updateQuestion(index: Int, question: String) {
        _questions[index] = _questions[index].copy(question = question)
    }

    fun updateAnswer(index: Int, answer: String) {
        _questions[index] = _questions[index].copy(answer = answer)
    }

    fun toData() = SecurityQuestionsData(questions = _questions.toList())

    fun validate(): ValidationResult {
        _questions.forEachIndexed { index, (question, answer) ->
            val num = index + 1
            if (question.isBlank()) {
                return ValidationResult.Error("请输入第${num}个安全问题")
            }
            if (answer.isBlank()) {
                return ValidationResult.Error("请输入第${num}个安全问题的答案")
            }
        }
        return ValidationResult.Success
    }

    fun isQuestionValid(index: Int): Boolean {
        val q = _questions[index]
        return q.question.isNotBlank() && q.answer.isNotBlank()
    }
}

private const val QUESTION_COUNT = 3

@Composable
fun SecurityQuestionsStep(
    state: SecurityQuestionsState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(QUESTION_COUNT) { index ->
            if (index > 0) {
                HorizontalDivider()
            }

            val question = state.getQuestion(index)
            val isLast = index == QUESTION_COUNT - 1

            SecurityQuestionItem(
                questionNumber = index + 1,
                question = question.question,
                onQuestionChange = { state.updateQuestion(index, it) },
                answer = question.answer,
                onAnswerChange = { state.updateAnswer(index, it) },
                isError = errorMessage != null && !state.isQuestionValid(index),
                onQuestionNext = { focusManager.moveFocus(FocusDirection.Down) },
                onAnswerNext = {
                    if (isLast) {
                        focusManager.clearFocus()
                    } else {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                },
                isLastQuestion = isLast
            )
        }
    }
}

@Composable
private fun SecurityQuestionItem(
    questionNumber: Int,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    isError: Boolean,
    onQuestionNext: () -> Unit,
    onAnswerNext: () -> Unit,
    isLastQuestion: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            isError = isError && question.isBlank(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onQuestionNext() }),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text("请输入答案") },
            isError = isError && answer.isBlank(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = if (isLastQuestion) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onAnswerNext() },
                onDone = { onAnswerNext() }
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}