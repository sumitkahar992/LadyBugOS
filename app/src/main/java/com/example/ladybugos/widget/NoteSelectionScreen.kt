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


@Composable
fun NoteSelectionContent(
    viewModel: NoteSelectionViewModel,
    onNoteSelected: (Note?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is NotesUiState.Loading -> LoadingScreen()
        is NotesUiState.Success -> {
            val notes = state.notes.filterNot { it.isArchived && it.isTrashed }
            if (notes.isEmpty()) EmptyState()
            else NoteSelectionScreen(notes, onNoteSelected)
        }

        is NotesUiState.Error -> ErrorScreen(state.message)
    }
}


@Composable
fun NoteSelectionScreen(
    notes: List<Note>,
    onNoteSelected: (note: Note?) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(notes.filter { !it.isArchived && !it.isTrashed }) { note ->
            NoteListItem(note) {
                onNoteSelected(note)
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
fun NoteListItem(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}