package com.example.ladybugos.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.NoteWithTags
import com.example.ladybugos.ui.components.ReminderInfo
import org.koin.androidx.compose.koinViewModel


@Composable
fun NoteSelectionContent(
    viewModel: NoteSelectionViewModel = koinViewModel(),
    widgetId: Int,
    onNoteSelected: (Note?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(NoteSelectionEvent.RefreshNotes)
    }

    when (val state = uiState) {

        is NoteSelectionUiState.Error -> ErrorScreen(state.message)
        NoteSelectionUiState.Loading -> LoadingScreen()
        is NoteSelectionUiState.Success -> {
            NoteSelectionContent(
                notes = state.notes,
                onNoteSelected = onNoteSelected,
                widgetId = widgetId
            )
        }
    }
}


@Composable
private fun NoteSelectionContent(
    notes: List<NoteWithTags>,
    onNoteSelected: (Note) -> Unit,
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
    notes: List<NoteWithTags>,
    onNoteSelected: (note: Note) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = notes,
            key = { it.note.id }
        ) { note ->
            NoteListItem(note) {
                onNoteSelected(note.note)
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
fun NoteListItem(
    noteWithTags: NoteWithTags,
    onNoteSelected: (Note) -> Unit

) {
    val note = noteWithTags.note
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


            note.reminderDate?.let { reminderDate ->
                Spacer(modifier = Modifier.height(8.dp))
                ReminderInfo(
                    reminderDate = reminderDate,
                    isDone = note.isDone,
                    onClick = {}
                )
            }
        }
    }
}