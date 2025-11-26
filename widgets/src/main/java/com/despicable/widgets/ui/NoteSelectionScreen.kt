package com.despicable.widgets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.despicable.core.designsystem.DarkNoteColors
import com.despicable.core.designsystem.LightNoteColors
import com.despicable.core.designsystem.component.ReminderInfo
import com.despicable.core.model.NoteComplete
import com.despicable.core.model.NoteContent
import org.koin.androidx.compose.koinViewModel


@Composable
fun NoteSelectionContent(
    viewModel: NoteSelectionViewModel = koinViewModel(),
    widgetId: Int,
    onNoteSelected: (NoteComplete?) -> Unit
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
                notes = state.notes,
                onNoteSelected = onNoteSelected,
                widgetId = widgetId
            )
        }
    }
}


@Composable
private fun NoteSelectionData(
    notes: List<NoteComplete>,
    onNoteSelected: (NoteComplete) -> Unit,
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesList(
    notes: List<NoteComplete>,
    onNoteSelected: (note: NoteComplete) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Select a note for Widgets",
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(
                items = notes,
                key = { it.note.id }
            ) { note ->

                NotePreview(noteComplete = note) {
                    onNoteSelected(note)
                }
            }
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


@Composable
fun NotePreview(
    modifier: Modifier = Modifier,
    noteComplete: NoteComplete,
    onNoteSelected: (NoteComplete) -> Unit
) {

    val note = noteComplete.note

    // Get the correct color from the palette based on the colorId
    val colorId = note.lightColor
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
            .padding(4.dp)
            .clickable(onClick = { onNoteSelected(noteComplete) }),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content - either checklist or regular note

            when (val content = note.content) {
                is NoteContent.ChecklistItems -> {
                    content.items.take(8).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) {
                                    Icons.Rounded.CheckBox
                                } else {
                                    Icons.Rounded.CheckBoxOutlineBlank
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (item.isChecked) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (item.isChecked) 0.6f else 1f
                                ),
                            )
                        }
                    }

                    if (content.items.size > 8) {
                        Text(
                            text = "+ ${content.items.size - 8} more items",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                is NoteContent.Text -> {
                    Text(
                        text = content.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 10,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            note.reminderDate?.let { reminderDate ->
                Spacer(modifier = Modifier.height(8.dp))
                ReminderInfo(
                    reminderDate = reminderDate,
                    isDone = note.isDone,
                    onClick = {},
                    noteColor = note.lightColor
                )
            }

        }
    }
}

/*

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


            */
/*       note.reminderDate?.let { reminderDate ->
                       Spacer(modifier = Modifier.height(8.dp))
                       ReminderInfo(
                           reminderDate = reminderDate,
                           isDone = note.isDone,
                           onClick = {}
                       )
                   }*//*

        }
    }
}


*/


