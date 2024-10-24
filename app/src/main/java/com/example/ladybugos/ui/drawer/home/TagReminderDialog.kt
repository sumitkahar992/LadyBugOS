package com.example.ladybugos.ui.drawer.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId


enum class GridLayout {
    OneColumn, TwoColumns, ThreeColumns
}

@Composable
fun LayoutSelectionDialog(
    currentLayout: GridLayout,
    onLayoutSelected: (GridLayout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Layout") },
        text = {
            Column {
                GridLayout.entries.forEach { layout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLayoutSelected(layout) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = layout == currentLayout,
                            onClick = { onLayoutSelected(layout) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (layout) {
                                GridLayout.OneColumn -> "Single Column"
                                GridLayout.TwoColumns -> "Two Columns"
                                GridLayout.ThreeColumns -> "Three Columns"
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            when (layout) {
                                GridLayout.OneColumn -> Icon(
                                    Icons.Default.ViewStream,
                                    contentDescription = null
                                )

                                GridLayout.TwoColumns -> Icon(
                                    Icons.Default.ViewModule,
                                    contentDescription = null
                                )

                                GridLayout.ThreeColumns -> Icon(
                                    Icons.Default.ViewComfy,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun ReminderDialog(
    showDialog: Boolean,
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onSetReminder: (Long?) -> Unit
) {
    var isDatePickerVisible by remember { mutableStateOf(true) }
    var selectedDate by remember {
        mutableStateOf(
            initialDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now()
        )
    }
    var selectedTime by remember {
        mutableStateOf(
            initialDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
            } ?: LocalTime.now()
        )
    }

    /*    val isDateValid = remember(selectedDate) {
            selectedDate >= LocalDate.now()
        }

        val isDateTimeValid = remember(selectedDate, selectedTime) {
            val selectedDateTime = selectedDate.atTime(selectedTime)
            selectedDateTime.atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli() > System.currentTimeMillis()
        }*/

    if (showDialog) {
        AnimatedContent(
            targetState = isDatePickerVisible,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            }, label = ""
        ) { isDatePicker ->
            if (isDatePicker) {
                DatePickerContent(
                    selectedDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                        isDatePickerVisible = false
                    },
                    onDismiss = onDismiss
                )
            } else {
                TimePickerContent(
                    selectedTime = selectedTime,
                    onTimeSelected = { newTime ->
                        selectedTime = newTime
                        val reminderMillis = selectedDate.atTime(newTime)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSetReminder(reminderMillis)
                    },
                    onBack = { isDatePickerVisible = true }
                )
            }
        }
    }
}



@Composable
fun AddTagDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Tag") },
        text = {
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tagName) },
                enabled = tagName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpdateTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onConfirm: (Tag) -> Unit,
    onDelete: () -> Unit
) {
    var updatedTagName by remember { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            Column {
                TextField(
                    value = updatedTagName,
                    onValueChange = { updatedTagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Tap outside to save changes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        "",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
                Button(onClick = {
                    onConfirm(tag.copy(name = updatedTagName))
                    onDismiss()
                }) {
                    Text("Update")
                }
            }
        }
    )
}