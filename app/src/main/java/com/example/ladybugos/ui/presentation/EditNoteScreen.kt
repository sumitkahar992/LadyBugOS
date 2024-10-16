package com.example.ladybugos.ui.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.RepeatInterval
import com.example.ladybugos.model.Tag
import com.example.ladybugos.model.colorPalette
import com.example.ladybugos.model.darken
import com.example.ladybugos.model.getRelativeTimeAgo
import com.example.ladybugos.ui.theme.Theme
import org.koin.androidx.compose.koinViewModel


@Composable
fun EditNoteScreen(
    viewModel: EditNoteViewModel = koinViewModel(),
    onBack: () -> Unit,
    onDelete: (id: Long?) -> Unit,
) {
    val note by viewModel.noteState.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val noteContent by viewModel.noteContent.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    val snackBarHostState = remember { SnackbarHostState() }
    var isColorPickerDialogVisible by remember { mutableStateOf(false) }

    val theme by viewModel.theme.collectAsStateWithLifecycle()


    val darkTheme = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }

    val defaultColor = MaterialTheme.colorScheme.surface
    val containerColor = remember(note.lightColor, darkTheme) {
        when {
            note.lightColor == 0 -> defaultColor
            darkTheme -> Color(note.lightColor).darken(0.4f)
            else -> Color(note.lightColor)
        }
    }

    // Ensure the note is deleted only if it's empty when disposing the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.deleteNoteIfEmpty()
        }
    }

    BackHandler {
        viewModel.saveNote(
            onComplete = onBack,
            onSkip = onBack
        )
    }

    var showReminderDialog by remember { mutableStateOf(false) }
    var showCompletionCard by remember { mutableStateOf(false) }


    Column {
        if (showCompletionCard) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    "Reminder completed!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }


    Scaffold(
        containerColor = containerColor,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            EditNoteTopAppBar(
                containerColor = containerColor,
                onTogglePin = {
                    viewModel.togglePinStatus()
                },
                onDelete = {
                    viewModel.deleteNoteAndUpdateLists { deletedId ->
                        onDelete(deletedId) // Trigger navigation back after deletion
                    }
                },
                isPinned = note.isPinned,
                onArchive = {
                    viewModel.archiveNoteAndUpdateLists { deletedId ->
                        onDelete(deletedId) // Trigger navigation back after archive
                    }
                },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        EditNoteContent(
            modifier = Modifier.padding(innerPadding),
            containerColor = containerColor,
            noteTitle = noteTitle,
            onTitleChange = {
                viewModel.updateNoteTitle(TextFieldValue(text = it))
            },
            noteContent = noteContent,
            onContentChange = {
                viewModel.updateNoteContent(TextFieldValue(text = it))
            },
            date = note.updateDate,
            onOpenColorPicker = { isColorPickerDialogVisible = true },
            reminderDate = note.reminderDate,
            onClickReminderInfo = {
                showReminderDialog = true
            },
            isDone = note.isDone,
            content = {
                LazyRow(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .width(LocalConfiguration.current.screenWidthDp.dp),
                    contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allTags) { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = selectedTagIds.contains(tag.id),
                            onClick = { viewModel.toggleTag(tag.id) },
                            onLongClick = { }
                        )
                    }
                }
            }
        )
        if (isColorPickerDialogVisible) {
            ColorPickerDialog(
                selectedColor = containerColor,
                onColorSelected = { selectedColor ->
                    viewModel.updateColor(newColor = selectedColor.toArgb())
                    isColorPickerDialogVisible = false
                },
                onDismissRequest = { isColorPickerDialogVisible = false }
            )
        }

        if (showReminderDialog) {
            ReminderDialog(
                showDialog = true,
                initialDate = note.reminderDate,
                onDismiss = { showReminderDialog = false },
                onSetReminder = { reminderDate ->
                    viewModel.updateNoteReminder(
                        noteId = note.id,
                        reminderDate = reminderDate
                    )
                    showReminderDialog = false
                },
            )
        }

    }
}

@Composable
fun EditTagHeader(
    tags: List<Tag>,
    selectedTagIds: List<Long?>,
    onTagClick: (Long) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .width(LocalConfiguration.current.screenWidthDp.dp),
        contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            TagChip(
                tag = tag,
                isSelected = selectedTagIds.contains(tag.id),
                onClick = { onTagClick(tag.id) },
                onLongClick = { }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteTopAppBar(
    containerColor: Color,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    isPinned: Boolean,
    onArchive: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { /* Text("Edit Note") */ },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = { onTogglePin() }) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin Note"
                )
            }
            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = "Archive Note"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Note"
                )
            }
        }
    )
}


@Composable
fun EditNoteContent(
    modifier: Modifier = Modifier,
    date: String,
    noteTitle: TextFieldValue,
    noteContent: TextFieldValue,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onOpenColorPicker: () -> Unit,
    containerColor: Color,
    reminderDate: Long?,
    isDone: Boolean,
    onClickReminderInfo: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .background(containerColor)
    ) {
        Column(
            modifier = Modifier
//                .padding(vertical = 8.dp)
                .fillMaxSize()
        ) {
            // Tag Header
            content()


            // Title and color picker row
            Row {
                NoteTextField(
                    value = noteTitle.text,
                    onValueChange = onTitleChange,
                    placeholder = "Title",
                    noteColor = containerColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                IconButton(
                    onClick = onOpenColorPicker,
                    modifier = Modifier.weight(0.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Open color picker",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content TextField
            NoteTextField(
                value = noteContent.text,
                onValueChange = onContentChange,
                placeholder = "Content",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                singleLine = false,
                noteColor = containerColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            reminderDate?.let { date ->
                ReminderInfo(
                    reminderDate = date,
                    onClick = onClickReminderInfo,
                    isDone = isDone
                )
            }

            Spacer(modifier = Modifier.height(32.dp))


            // Edited date text
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Edited ${getRelativeTimeAgo(date)}",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun TagSelector(
    allTags: List<Tag>,
    selectedTags: List<Tag>,
    onTagToggle: (Tag) -> Unit
) {
    // Move selected tags to the top
    val sortedTags = allTags.sortedByDescending { it in selectedTags }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sortedTags) { tag ->
            TagChipNew(
                tag = tag,
                isSelected = tag in selectedTags,
                onClick = { onTagToggle(tag) }
            )
        }
    }
}


@Composable
fun TagChipNew(tag: Tag, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        Color(tag.color).copy(alpha = 0.5f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        contentColorFor(backgroundColor)
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = tag.name,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}


@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

}


@Composable
fun ColorPickerDialog(
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit,
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


@Preview(showBackground = true)
@Composable
fun ColorPickerDialogPreview() {
    var selectedColor by remember { mutableStateOf(Color.Red) } // Example selected color

    ColorPickerDialog(
        selectedColor = selectedColor,
        onColorSelected = { color ->
            selectedColor = color // Update the selected color
        },
        onDismissRequest = { /* Handle dismiss action */ }
    )
}


@Composable
fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String,
    noteColor: Color
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        placeholder = if (value.isEmpty()) {
            { Text(placeholder) }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = noteColor,
            unfocusedContainerColor = noteColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
    )
}
