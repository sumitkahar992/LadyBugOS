package com.despicable.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReminderDialog(
    showDialog: Boolean,
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onSetReminder: (Long?) -> Unit,
    onDeleteReminder: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ReminderTab.TIME) }
    var showTimeMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPastDateError by remember { mutableStateOf(false) }

    // Initialize with initial date if provided
    val initialDateTime = remember(initialDate) {
        initialDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
        }
    }

    var selectedTime by remember {
        mutableStateOf(
            if (initialDateTime != null) {
                TimeOption.PICK_TIME
            } else {
                TimeOption.MORNING
            }
        )
    }
    var customTime by remember {
        mutableStateOf(
            initialDateTime?.toLocalTime()
        )
    }

    var selectedDate by remember {
        mutableStateOf(
            if (initialDateTime != null) {
                DateOption.PICK_DATE
            } else {
                DateOption.TODAY
            }
        )
    }
    var customDate by remember {
        mutableStateOf(
            initialDateTime?.toLocalDate()
        )
    }

    var selectedRepeat by remember { mutableStateOf(RepeatOption.DOES_NOT_REPEAT) }

    // Calculate if selected date/time is in the past
    val selectedDateTime = remember(selectedDate, selectedTime, customDate, customTime) {
        val date = customDate ?: selectedDate.getLocalDate()
        val time = customTime ?: selectedTime.getLocalTime()
        date.atTime(time).atZone(ZoneId.systemDefault())
    }
    val isInPast = selectedDateTime.toInstant().toEpochMilli() < System.currentTimeMillis()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit reminder") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Tabs with divider
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            TabButton(
                                selected = selectedTab == ReminderTab.TIME,
                                onClick = { selectedTab = ReminderTab.TIME },
                                text = "Time",
                                modifier = Modifier.weight(1f)
                            )
                            TabButton(
                                selected = selectedTab == ReminderTab.DATE,
                                onClick = { selectedTab = ReminderTab.DATE },
                                text = "Date",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        ReminderTab.TIME -> {
                            Box {
                                Column {
                                    MenuButton(
                                        text = when {
                                            customTime != null -> customTime!!.format(
                                                DateTimeFormatter.ofPattern("h:mm a")
                                            )

                                            else -> selectedTime.label
                                        },
                                        secondaryText = when {
                                            customTime != null -> null
                                            else -> selectedTime.timeString
                                        },
                                        onClick = { showTimeMenu = true }
                                    )
                                    MenuButton(
                                        text = selectedRepeat.label,
                                        onClick = { showRepeatMenu = true }
                                    )
                                }

                                // Time Menu
                                if (showTimeMenu) {
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { showTimeMenu = false },
                                        modifier = Modifier
                                            .width(LocalConfiguration.current.screenWidthDp.dp - 80.dp)
                                            .heightIn(max = 280.dp)
                                            .offset(y = (-8).dp),
                                    ) {
                                        TimeOption.entries.forEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(option.label)
                                                        if (option != TimeOption.PICK_TIME) {
                                                            Text(
                                                                option.timeString,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    if (option == TimeOption.PICK_TIME) {
                                                        showTimePicker = true
                                                    } else {
                                                        selectedTime = option
                                                        customTime = null
                                                    }
                                                    showTimeMenu = false
                                                }
                                            )
                                            if (index < TimeOption.entries.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.12f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Repeat Menu
                                if (showRepeatMenu) {
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { showRepeatMenu = false },
                                        modifier = Modifier
                                            .width(LocalConfiguration.current.screenWidthDp.dp - 80.dp)
                                            .heightIn(max = 280.dp)
                                            .offset(y = (-8).dp),
                                    ) {
                                        RepeatOption.entries.forEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                text = { Text(option.label) },
                                                onClick = {
                                                    selectedRepeat = option
                                                    showRepeatMenu = false
                                                }
                                            )
                                            if (index < RepeatOption.entries.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.12f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ReminderTab.DATE -> {
                            Box {
                                MenuButton(
                                    text = when {
                                        customDate != null -> customDate!!.format(
                                            DateTimeFormatter.ofPattern("MMMM dd")
                                        )

                                        else -> selectedDate.getDisplayText()
                                    },
                                    onClick = { showDateMenu = true }
                                )

                                // Date Menu
                                if (showDateMenu) {
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { showDateMenu = false },
                                        modifier = Modifier
                                            .width(LocalConfiguration.current.screenWidthDp.dp - 80.dp)
                                            .heightIn(max = 280.dp)
                                            .offset(y = (-8).dp),
                                    ) {
                                        DateOption.entries.forEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                text = { Text(option.getDisplayText()) },
                                                onClick = {
                                                    if (option == DateOption.PICK_DATE) {
                                                        showDatePicker = true
                                                    } else {
                                                        selectedDate = option
                                                        customDate = null
                                                    }
                                                    showDateMenu = false
                                                }
                                            )
                                            if (index < DateOption.entries.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.12f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Show error message only when user tries to save a past date
                    if (showPastDateError && isInPast) {
                        Text(
                            text = "The date has passed",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onDeleteReminder(); onDismiss() }) {
                        Text("Delete")
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isInPast) {
                                    showPastDateError = true
                                } else {
                                    val time = customTime ?: selectedTime.getLocalTime()
                                    val date = customDate ?: selectedDate.getLocalDate()
                                    val reminderMillis = date.atTime(time)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                    onSetReminder(reminderMillis)
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        )
    }

    // Pickers
    if (showDatePicker) {
        DatePickerContent(
            selectedDate = customDate ?: selectedDate.getLocalDate(),
            onDateSelected = { date ->
                customDate = date
                selectedDate = DateOption.PICK_DATE
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerContent(
            selectedTime = customTime ?: selectedTime.getLocalTime(),
            onTimeSelected = { time ->
                customTime = time
                selectedTime = TimeOption.PICK_TIME
                showTimePicker = false
            },
            onBack = { showTimePicker = false },
            isCurrentDate = (customDate ?: selectedDate.getLocalDate()).isEqual(LocalDate.now())
        )
    }
}

private enum class TimeOption(val label: String, val timeString: String) {
    MORNING("Morning", "8:00 AM"),
    AFTERNOON("Afternoon", "1:00 PM"),
    EVENING("Evening", "6:00 PM"),
    NIGHT("Night", "8:00 PM"),
    PICK_TIME("Pick a time...", "");

    fun getLocalTime(): LocalTime = when (this) {
        MORNING -> LocalTime.of(8, 0)
        AFTERNOON -> LocalTime.of(13, 0)
        EVENING -> LocalTime.of(18, 0)
        NIGHT -> LocalTime.of(20, 0)
        PICK_TIME -> LocalTime.now()
    }
}

private enum class DateOption {
    TODAY,
    TOMORROW,
    NEXT_WEEK,
    PICK_DATE;

    fun getDisplayText(): String = when (this) {
        TODAY -> "Today"
        TOMORROW -> "Tomorrow"
        NEXT_WEEK -> "Next ${
            LocalDate.now().plusWeeks(1).dayOfWeek.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }"

        PICK_DATE -> "Pick a date..."
    }

    fun getLocalDate(): LocalDate = when (this) {
        TODAY -> LocalDate.now()
        TOMORROW -> LocalDate.now().plusDays(1)
        NEXT_WEEK -> LocalDate.now().plusWeeks(1)
        PICK_DATE -> LocalDate.now()
    }
}

private enum class RepeatOption(val label: String) {
    DOES_NOT_REPEAT("Does not repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

@Composable
private fun TabButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
        if (selected) {
            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    secondaryText: String? = null,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }
}

private enum class ReminderTab {
    TIME, DATE
}
