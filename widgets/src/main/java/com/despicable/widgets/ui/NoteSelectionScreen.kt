package com.despicable.widgets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.despicable.core.designsystem.DarkNoteColors
import com.despicable.core.designsystem.LightNoteColors
import com.despicable.core.model.getRelativeTimeAgo
import com.despicable.widgets.model.WidgetNote
import org.koin.androidx.compose.koinViewModel


@Composable
fun NoteSelectionContent(
    viewModel: NoteSelectionViewModel = koinViewModel(),
    widgetId: Int,
    onNoteSelected: (WidgetNote?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(NoteSelectionEvent.RefreshNotes)
    }

    when (val state = uiState) {

        is NoteSelectionUiState.Error -> ErrorScreen(state.message)
        NoteSelectionUiState.Loading -> LoadingScreen()
        is NoteSelectionUiState.Success -> {
            NoteSelectionData(
                notes = state.notes.map { it.toWidgetNote() },
                onNoteSelected = onNoteSelected,
                widgetId = widgetId
            )
        }
    }
}


@Composable
private fun NoteSelectionData(
    notes: List<WidgetNote>,
    onNoteSelected: (WidgetNote) -> Unit,
    widgetId: Int,
) {
    if (notes.isEmpty()) {
        EmptyState()
    } else {
        NotesList(
            notes = notes,
            onNoteSelected = onNoteSelected,
        )
    }
}


@Composable
fun NotesList(
    notes: List<WidgetNote>,
    onNoteSelected: (note: WidgetNote) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = notes,
            key = { it.id }
        ) { note ->

            NotePreview(note = note) {
                onNoteSelected(note)
            }

//            NoteListItem(note) {
//                onNoteSelected(note)
//            }
        }
    }
}


@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No notes available", style = TextStyle(fontSize = 20.sp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Error: $message")
    }
}


// In your ConfigActivity.kt file, update the preview section

@Composable
fun NotePreview(
    modifier: Modifier = Modifier,
    note: WidgetNote,
    onNoteSelected: (WidgetNote) -> Unit
) {
    // Get the correct color from the palette based on the colorId
    val colorId = note.color
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor =
        if (colorId >= 0 && colorId < (if (isDarkTheme) DarkNoteColors else LightNoteColors).size) {
            if (isDarkTheme) DarkNoteColors[colorId] else LightNoteColors[colorId]
        } else {
            // Fallback
            if (isDarkTheme) Color(0xFF202124) else Color.White
        }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = { onNoteSelected(note) }),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content - either checklist or regular note
            if (note.isChecklist) {
                // Show checklist items
                note.checklistItems.take(6).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isChecked)
                                Icons.Filled.CheckBox
                            else
                                Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.isChecked)
                                TextDecoration.LineThrough
                            else
                                null,
                            color = if (item.isChecked)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Show "more items" if there are more than 3
                if (note.checklistItems.size > 6) {
                    Text(
                        text = "+ ${note.checklistItems.size - 6} more items",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                // Regular note content
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Last updated
            Text(
                text = "Last updated: ${getRelativeTimeAgo(note.lastUpdate.toLong())}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.End
            )
        }
    }
}


@Composable
fun NoteListItem(
    note: WidgetNote,
    onNoteSelected: (WidgetNote) -> Unit

) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = { onNoteSelected(note) }),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(4.dp))


            /*       note.reminderDate?.let { reminderDate ->
                       Spacer(modifier = Modifier.height(8.dp))
                       ReminderInfo(
                           reminderDate = reminderDate,
                           isDone = note.isDone,
                           onClick = {}
                       )
                   }*/
        }
    }
}


