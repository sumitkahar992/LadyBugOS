package com.despicable.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerContent(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
// Convert LocalDate to Instant for the picker state using the system default time zone
    val initialInstant = selectedDate.atStartOfDayAtDefaultTimeZone()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialInstant.toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date =
                    Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateAtDefaultTimeZone()
                return date >= Clock.System.todayAtDefaultTimeZone()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newSelectedDate =
                            Instant.fromEpochMilliseconds(it).toLocalDateAtDefaultTimeZone()
                        onDateSelected(newSelectedDate)
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            showModeToggle = false,
        )
    }
}


// Helper functions for time zone handling
fun LocalDate.atStartOfDayAtDefaultTimeZone(): Instant =
    this.atTime(LocalTime(0, 0)).toInstant(TimeZone.currentSystemDefault())

fun Instant.toLocalDateAtDefaultTimeZone(): LocalDate =
    this.toLocalDateTime(TimeZone.currentSystemDefault()).date

fun Clock.System.todayAtDefaultTimeZone(): LocalDate =
    this.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerContent(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onBack: () -> Unit,
    isCurrentDate: Boolean = false
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    // Check if selected time is valid (not in the past for current date)
    val selectedTimeIsValid =
        remember(timePickerState.hour, timePickerState.minute, isCurrentDate) {
            if (!isCurrentDate) true
            else {
                val currentTime =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
                val selectedTimes = LocalTime(timePickerState.hour, timePickerState.minute)
                selectedTimes >= currentTime
            }
        }

    TimePickerDialog(
        onDismiss = onBack,
        onConfirm = {
            val newSelectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
            onTimeSelected(newSelectedTime)
        },
        confirmEnabled = selectedTimeIsValid,
        showError = !selectedTimeIsValid
    ) {
        Column {
            TimePicker(state = timePickerState)
            if (!selectedTimeIsValid) {
                Text(
                    text = "The time has passed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    showError: Boolean = false,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.height(12.dp))

                content()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Back") }
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled
                    ) {
                        Text("Set Reminder")
                    }
                }
            }
        }
    }
}
