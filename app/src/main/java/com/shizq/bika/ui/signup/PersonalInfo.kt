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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.ValidationResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

enum class Gender(val displayName: String) {
    MALE("男"),
    FEMALE("女"),
    ROBOT("机器人")
}

data class PersonalInfoData(
    val birthday: LocalDate,
    val gender: Gender
)

class PersonalInfoState {
    var birthday by mutableStateOf(defaultBirthday())
    var gender by mutableStateOf(Gender.MALE)
    var showDatePicker by mutableStateOf(false)
    val age: Int
        get() = calculateAge(birthday, today())
    fun toData() = PersonalInfoData(
        birthday = birthday,
        gender = gender
    )
    fun validate(): ValidationResult {
        if (age < MIN_AGE) {
            return ValidationResult.Error("您必须年满${MIN_AGE}岁才能注册")
        }
        return ValidationResult.Success
    }

    companion object {
        private const val MIN_AGE = 18
        private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        private fun defaultBirthday(): LocalDate {
            val today = today()
            return LocalDate(today.year - MIN_AGE, today.month.number, today.day)
        }

        private fun calculateAge(birthday: LocalDate, today: LocalDate): Int {
            var age = today.year - birthday.year
            if (today.month.number < birthday.month.number ||
                (today.month.number == birthday.month.number && today.day < birthday.day)
            ) {
                age--
            }
            return age
        }
    }
}

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
        BirthdaySelector(
            birthday = state.birthday,
            onBirthdayClick = { state.showDatePicker = true },
            age = state.age,
            isError = errorMessage != null
        )

        GenderSelector(
            selectedGender = state.gender,
            onGenderSelected = { state.gender = it }
        )
    }

    if (state.showDatePicker) {
        BirthdayDatePickerDialog(
            initialDate = state.birthday,
            onDateSelected = { state.birthday = it },
            onDismiss = { state.showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                        onDateSelected(selectedDate)
                    }
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun BirthdaySelector(
    birthday: LocalDate,
    onBirthdayClick: () -> Unit,
    age: Int,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
                containerColor = if (isError) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
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
                    Text(
                        text = birthday.formatChinese(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "年龄：$age 岁",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (age >= 18) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
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

private val ChineseDateFormat = LocalDate.Format {
    year()
    chars("年")
    monthNumber()
    chars("月")
    day()
    chars("日")
}

private fun LocalDate.formatChinese(): String = format(ChineseDateFormat)

@Composable
private fun GenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
                    onClick = { onGenderSelected(gender) }
                )
            }
        }
    }
}

@Composable
private fun GenderOption(
    gender: Gender,
    selected: Boolean,
    onClick: () -> Unit,
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
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
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
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}
