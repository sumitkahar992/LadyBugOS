package com.despicable.feature.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.component.DatePickerContent
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.component.ReminderDialog
import com.despicable.core.designsystem.component.TimePickerContent
import com.despicable.core.designsystem.theme.GridLayout
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import java.time.LocalDate
import java.time.LocalTime


@Composable
fun LayoutSelectionDialog(
    enabled: Boolean,
    currentLayout: GridLayout,
    onLayoutSelected: (GridLayout) -> Unit,
    onDismiss: () -> Unit
) {
    NoteeDialog(
        enabled = enabled,
        onDismiss = onDismiss,
        title = stringResource(R.string.home_searchbar_select_layout),
        description = {
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
                                GridLayout.OneColumn -> stringResource(R.string.home_layout_single_column)
                                GridLayout.TwoColumns -> stringResource(R.string.home_layout_two_columns)
                                GridLayout.ThreeColumns -> stringResource(R.string.home_layout_three_columns)
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
        }
    )
}

/*    val isDateValid = remember(selectedDate) {
            selectedDate >= LocalDate.now()
        }

        val isDateTimeValid = remember(selectedDate, selectedTime) {
            val selectedDateTime = selectedDate.atTime(selectedTime)
            selectedDateTime.atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli() > System.currentTimeMillis()
        }
        */



@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
private fun ReminderDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }

    LadyBugOSTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReminderDialog(
                showDialog = showDialog,
                initialDate = System.currentTimeMillis(),
                onDismiss = { showDialog = false },
                onSetReminder = { },
                onDeleteReminder = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimePickerPreview() {
    LadyBugOSTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TimePickerContent(
                selectedTime = LocalTime.now(),
                onTimeSelected = {},
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePickerPreview() {
    LadyBugOSTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DatePickerContent(
                selectedDate = LocalDate.now(),
                onDateSelected = {},
                onDismiss = {}
            )
        }
    }
}
