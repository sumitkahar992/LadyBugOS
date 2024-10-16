package com.example.ladybugos.claude_note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.model.darken
import com.example.ladybugos.ui.presentation.ColorItem
import com.example.ladybugos.ui.theme.Theme
import org.koin.androidx.compose.koinViewModel

@Composable
fun KNoteListScreen(
    viewModel: KNoteListViewModel = koinViewModel(),
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit
) {
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    var showUpdateTagDialog by remember { mutableStateOf(false) }
    var tagToUpdate by remember { mutableStateOf<KTag?>(null) }
    var updatedTagName by remember { mutableStateOf("") }

    var showDeleteNoteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<KNote?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
//        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { pv ->
        Column(Modifier.padding(paddingValues = pv)) {
            // Tag header
            KTagHeader(
                tags = tags,
                selectedTagId = selectedTagId,
                onTagClick = { tagId -> viewModel.toggleTag(tagId) },
                onTagLongClick = { tag ->
                    tagToUpdate = tag
                    updatedTagName = tag.name
                    showUpdateTagDialog = true
                },
                onAddTagClick = { showAddTagDialog = true }
            )

            // Note list
            KNoteList(
                notes = filteredNotes,
                onNoteClick = onNoteClick,
                onNoteLongClick = { note ->
                    noteToDelete = note
                    showDeleteNoteDialog = true
                }
            )
        }
    }
    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                newTagName = ""
            },
            title = { Text("Add New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.addTag(newTagName)
                            showAddTagDialog = false
                            newTagName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTagDialog = false
                        newTagName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update/Delete Tag Dialog
    UpdateDeleteTagDialog(
        showDialog = showUpdateTagDialog,
        onDismiss = {
            showUpdateTagDialog = false
            tagToUpdate = null
        },
        tagToUpdate = tagToUpdate,
        onUpdateTag = { updatedTag ->
            viewModel.updateTag(updatedTag)
        },
        onDeleteTag = { tag ->
            viewModel.deleteTag(tag)
        }
    )

    // Delete Note Dialog
    if (showDeleteNoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteNoteDialog = false
                noteToDelete = null
            },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete?.let { note ->
                            viewModel.deleteNote(note)
                            showDeleteNoteDialog = false
                            noteToDelete = null
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteNoteDialog = false
                        noteToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UpdateDeleteTagDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    tagToUpdate: KTag?,
    onUpdateTag: (KTag) -> Unit,
    onDeleteTag: (KTag) -> Unit
) {
    if (showDialog && tagToUpdate != null) {
        var updatedTagName by remember { mutableStateOf(tagToUpdate.name) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Edit Tag")
                    TextButton(
                        onClick = {
                            onDeleteTag(tagToUpdate)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

            },
            text = {
                Column {
                    OutlinedTextField(
                        value = updatedTagName,
                        onValueChange = {
                            updatedTagName = it
                            onUpdateTag(tagToUpdate.copy(name = it))
                        },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            },
            confirmButton = {

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Close",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun UpdateTagPrev() {
    UpdateDeleteTagDialog(
        showDialog = true,
        onDismiss = {},
        tagToUpdate = KTag(1, "Test Tag"),
        onUpdateTag = {},
        onDeleteTag = {}
    )
}

@Composable
fun KTagHeader(
    tags: List<KTag>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
    onTagLongClick: (KTag) -> Unit,
    onAddTagClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        items(tags) { tag ->
            KTagChip(
                tag = tag,
                isSelected = tag.id == selectedTagId,
                onClick = { onTagClick(tag.id) },
                onLongClick = { onTagLongClick(tag) }
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onAddTagClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Add",
                        modifier = Modifier
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp)
                    )
                }

            }
        }
    }
}

@Preview
@Composable
private fun TagHeaderPrev() {
    KTagHeader(
        tags = listOf(KTag(1, "WORKEED"), KTag(2, "PERSONAL"), KTag(3, "STUDY")),
        selectedTagId = 0,
        onTagClick = {},
        onTagLongClick = {},
        onAddTagClick = {}
    )
}

@Composable
fun KNoteList(
    notes: List<KNoteWithTags>,
    onNoteClick: (Long) -> Unit,
    onNoteLongClick: (KNote) -> Unit
) {
    LazyColumn {
        items(notes) { noteWithTags ->
            KNoteItem(
                note = noteWithTags.note,
                tags = noteWithTags.tags,
                onClick = { onNoteClick(noteWithTags.note.id) },
                onLongClick = { onNoteLongClick(noteWithTags.note) }
            )
        }
    }
}

@Composable
fun KEditNoteScreen(
    viewModel: KEditNoteViewModel = koinViewModel(),
    noteId: Long?,
    onSaveComplete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val noteState by viewModel.noteState.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val noteContent by viewModel.noteContent.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    var isColorPickerDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0L) {
            viewModel.loadNote(noteId)
        }
    }

    BackHandler {
        viewModel.saveNote(
            onComplete = onSaveComplete,
            onSkip = onNavigateBack
        )
    }
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val darkTheme = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }
    val defaultColor = MaterialTheme.colorScheme.surface
    val containerColor = remember(noteState.lightColor, darkTheme) {
        when {
            noteState.lightColor == 0 -> defaultColor
            darkTheme -> Color(noteState.lightColor).darken(0.4f)
            else -> Color(noteState.lightColor)
        }
    }

    KEditNoteContent(
        modifier = Modifier.padding(16.dp),
        containerColor = Color(noteState.lightColor),
        noteTitle = noteTitle,
        onTitleChange = { viewModel.updateNoteTitle(it) },
        noteContent = noteContent,
        onContentChange = { viewModel.updateNoteContent(it) },
        date = noteState.updateDate,
        onOpenColorPicker = { isColorPickerDialogVisible = true },
        content = {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allTags) { tag ->
                    KTagChip(
                        tag = tag,
                        isSelected = selectedTagIds.contains(tag.id),
                        onClick = { viewModel.toggleTag(tag.id) },
                        onLongClick = {} // Implement if needed
                    )
                }
            }
        },
        isPinned = noteState.isPinned,
        onPinToggle = viewModel::togglePinned,
        isArchived = noteState.isArchived,
        onArchiveToggle = viewModel::toggleArchived,
        isTrashed = noteState.isTrashed,
        onTrashToggle = viewModel::toggleTrashed,
        onSaveClick = {
            viewModel.saveNote(
                onComplete = onSaveComplete,
                onSkip = onNavigateBack
            )
        }
    )

    if (isColorPickerDialogVisible) {
        com.example.ladybugos.ui.presentation.ColorPickerDialog(
            selectedColor = containerColor,
            onColorSelected = { selectedColor ->
                viewModel.updateColor(newColor = selectedColor.toArgb())
                isColorPickerDialogVisible = false
            },
            onDismissRequest = { isColorPickerDialogVisible = false }
        )
    }
}

@Composable
fun KEditNoteContent(
    modifier: Modifier = Modifier,
    containerColor: Color,
    noteTitle: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    noteContent: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    date: String,
    onOpenColorPicker: () -> Unit,
    content: @Composable () -> Unit,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    isArchived: Boolean,
    onArchiveToggle: () -> Unit,
    isTrashed: Boolean,
    onTrashToggle: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        OutlinedTextField(
            value = noteTitle,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = noteContent,
            onValueChange = onContentChange,
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Last updated: $date",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        content()

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPinToggle) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Toggle Pin"
                )
            }
            IconButton(onClick = onArchiveToggle) {
                Icon(
                    imageVector = if (isArchived) Icons.Filled.Archive else Icons.Outlined.Archive,
                    contentDescription = "Toggle Archive"
                )
            }
            IconButton(onClick = onTrashToggle) {
                Icon(
                    imageVector = if (isTrashed) Icons.Filled.Delete else Icons.Outlined.Delete,
                    contentDescription = "Toggle Trash"
                )
            }
            IconButton(onClick = onOpenColorPicker) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "Change Color"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSaveClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}

@Composable
fun ColorPickerDialog(
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Track whether the color list is expanded
    var isExpanded by remember { mutableStateOf(false) }

    // Limit to show initially 12 items
    val displayedColors = if (isExpanded) colorPalette else colorPalette.take(11)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            if (!isExpanded) {
                Text("Note color")
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display the regular colors
                items(displayedColors) { color ->
                    ColorItem(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = {
                            onColorSelected(color)
                        }
                    )
                }

                // Add the "Expand" button as the last item
                if (!isExpanded) {
                    item {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { isExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun KTagChip(
    tag: KTag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = tag.name,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun KTagChip(
    tag: KTag,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
        modifier = Modifier
            .padding(end = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        Text(
            text = tag.name,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KNoteItem(
    note: KNote,
    tags: List<KTag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow {
                tags.forEach { tag ->
                    KTagChip(
                        tag = tag,
                        isSelected = false,
                        onClick = {},
                        onLongClick = {}
                    )
                }
            }
        }
    }
}