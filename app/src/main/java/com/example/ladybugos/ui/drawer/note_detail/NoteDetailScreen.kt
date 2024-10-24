package com.example.ladybugos.ui.drawer.note_detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.darken
import com.example.ladybugos.model.getRelativeTimeAgo
import com.example.ladybugos.ui.components.ReminderInfo
import com.example.ladybugos.ui.components.TagChip
import com.example.ladybugos.ui.drawer.home.ReminderDialog
import com.example.ladybugos.ui.theme.Theme
import org.koin.androidx.compose.koinViewModel


@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
    onDelete: (id: Long?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }
    var isColorPickerDialogVisible by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val darkTheme = when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }

    val containerColor = when {
        uiState.lightColor == 0 -> MaterialTheme.colorScheme.surface
        darkTheme -> remember(uiState.lightColor) { Color(uiState.lightColor).darken(0.4f) }
        else -> remember(uiState.lightColor) { Color(uiState.lightColor) }
    }


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

    Scaffold(
        containerColor = containerColor,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            EditNoteTopAppBar(
                containerColor = containerColor,
                onTogglePin = viewModel::togglePinStatus,
                onDelete = {
                    viewModel.deleteNoteAndUpdateLists(onDelete)
                },
                isPinned = uiState.isPinned,
                onArchive = {
                    viewModel.archiveNoteAndUpdateLists(onDelete)
                },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        EditNoteContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            containerColor = containerColor,
            onTitleChange = viewModel::updateNoteTitle,
            onContentChange = viewModel::updateNoteContent,
            onOpenColorPicker = { isColorPickerDialogVisible = true },
            onClickReminderInfo = { showReminderDialog = true },
            isDone = uiState.isDone,
            content = {
                LazyRow(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .width(LocalConfiguration.current.screenWidthDp.dp),
                    contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.allTags) { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = uiState.selectedTagIds.contains(tag.id),
                            onClick = { viewModel.toggleTag(tag.id) },
                            onLongClick = { }
                        )
                    }
                }
            },
        )

        if (isColorPickerDialogVisible) {
            ColorPickerDialog(
                selectedColor = containerColor,
                onColorSelected = { selectedColor ->
                    viewModel.updateColor(selectedColor.toArgb())
                    isColorPickerDialogVisible = false
                },
                onDismissRequest = { isColorPickerDialogVisible = false }
            )
        }

        if (showReminderDialog) {
            ReminderDialog(
                showDialog = true,
                initialDate = uiState.reminderDate,
                onDismiss = { showReminderDialog = false },
                onSetReminder = { reminderDate ->
                    viewModel.updateNoteReminder(
                        noteId = uiState.id,
                        reminderDate = reminderDate
                    )
                    showReminderDialog = false
                },
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
    uiState: NoteUiState,
    containerColor: Color,
    onTitleChange: (TextFieldValue) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onOpenColorPicker: () -> Unit,
    onClickReminderInfo: () -> Unit,
    isDone: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .background(containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            content()

            Row {
                NoteTextField(
                    value = uiState.title.text,
                    onValueChange = { onTitleChange(TextFieldValue(it)) },
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

            NoteTextField(
                value = uiState.content.text,
                onValueChange = { onContentChange(TextFieldValue(it)) },
                placeholder = "Content",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                singleLine = false,
                noteColor = containerColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            uiState.reminderDate?.let { date ->
                ReminderInfo(
                    reminderDate = date,
                    isDone = isDone,
                    onClick = onClickReminderInfo
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Edited ${getRelativeTimeAgo(uiState.updateDate)}",
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
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
