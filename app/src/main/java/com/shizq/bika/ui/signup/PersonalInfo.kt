package com.shizq.bika.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.ValidationResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// 性别枚举
enum class Gender(val displayName: String) {
    MALE("男"),
    FEMALE("女"),
    ROBOT("机器人")
}

// 个人信息数据类
data class PersonalInfoData(
    val birthday: LocalDate,
    val gender: Gender
)

// 个人信息状态管理
class PersonalInfoState {
    private val current = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var birthday by mutableStateOf(current)
    var gender by mutableStateOf(Gender.MALE)
    var showDatePicker by mutableStateOf(false)

    fun toData() = PersonalInfoData(
        birthday = birthday,
        gender = gender
    )

    fun validate(): ValidationResult {
        val period = birthday - current
        if (period.years < 18) {
            return ValidationResult.Error("您必须年满18岁才能注册")
        }

        return ValidationResult.Success
    }

    fun getAge(): Int {
        return (current - birthday).years
    }
}

// 个人信息输入组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoStep(
    state: PersonalInfoState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 生日选择
        BirthdaySelector(
            birthday = state.birthday,
            onBirthdayClick = { state.showDatePicker = true },
            age = state.getAge(),
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        // 性别选择
        GenderSelector(
            selectedGender = state.gender,
            onGenderSelected = { state.gender = it },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 日期选择器对话框
    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.birthday
                .atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        )

        state.birthday
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
        DatePickerDialog(
            onDismissRequest = { state.showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            state.birthday = selectedDate
                        }
                        state.showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// 生日选择器
@Composable
fun BirthdaySelector(
    birthday: LocalDate,
    onBirthdayClick: () -> Unit,
    age: Int,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "生日",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedCard(
            onClick = onBirthdayClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isError)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = if (isError)
                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                else
                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val format = birthday.format(
                        LocalDate.Format {
                            year()
                            chars("年")
                            monthNumber()
                            chars("月")
                            day()
                            chars("日")
                        }
                    )
                    Text(
                        text = format,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (age != null) {
                        Text(
                            text = "年龄：$age 岁",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (age >= 18)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "选择日期",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "您必须年满18岁才能注册",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 性别选择器
@Composable
fun GenderSelector(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "性别",
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Gender.entries.forEach { gender ->
                GenderOption(
                    gender = gender,
                    selected = selectedGender == gender,
                    onClick = { onGenderSelected(gender) },
                    isError = isError
                )
            }
        }
    }
}

// 性别选项
@Composable
fun GenderOption(
    gender: Gender,
    selected: Boolean,
    onClick: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = when {
                selected -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                isError -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                else -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gender.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}