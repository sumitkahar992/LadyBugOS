package com.example.ladybugos.ui.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ladybugos.model.Note
import com.example.ladybugos.model.Tag
import com.example.ladybugos.ui.drawer.SwipeToDismissContentSnack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset


@Composable
fun NoteListScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = koinViewModel(),
    navigateToNoteDetail: (Long) -> Unit,
    onMenuClick: () -> Unit,
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val isSearchBarVisible = remember { mutableStateOf(true) }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showUpdateTagDialog by remember { mutableStateOf(false) }
    var tagToUpdate by remember { mutableStateOf<Tag?>(null) }
    var updatedTagName by remember { mutableStateOf("") }

    val originalNote by viewModel.originalNote.collectAsState()


    fun handleAction(action: (Set<Note>) -> Unit, message: String, restoreAction: () -> Unit) {
        action(selectedNotes)
        scope.launch {
            val result = snackBarHostState.showSnackbar(message, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                restoreAction()
            }
        }
        selectedNotes = emptySet()
    }

    val handlePinNotes: () -> Unit = {
        val allPinned = selectedNotes.all { it.isPinned }
        viewModel.run { if (allPinned) ::unpinNotes else ::pinNotes }(selectedNotes.toList())
        selectedNotes = emptySet()
    }

    fun toggleSelection(note: Note) {
        selectedNotes = selectedNotes.toMutableSet().apply {
            if (contains(note)) remove(note) else add(note)
        }
    }

    fun handleNoteClick(note: Note) {
        if (selectedNotes.isNotEmpty()) toggleSelection(note)
        else navigateToNoteDetail(note.id)
    }
    HandleOriginalNote(originalNote, snackBarHostState, viewModel)

    Timber.tag("DEBUG").d("[TAGS]=[$tags]")
    var showReminderDialog by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                SwipeToDismissContentSnack(
                    onSwipeToDismiss = { snackBarHostState.currentSnackbarData?.dismiss() },
                    content = { Snackbar(snackbarData = data) }
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigateToNoteDetail(-1L) },
                modifier = Modifier.padding(bottom = 22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("New Note", fontSize = 15.sp)
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .scrollConnectionToProvideVisibility(isSearchBarVisible)
        ) {
            NoteGridTags(
                notes = notes,
                selectedNotes = selectedNotes,
                onNoteClick = ::handleNoteClick,
                onNoteLongPress = ::toggleSelection,
                theme = theme,
                gridContent = {

                    tagHeader(
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
                }
            )
            ExpandableSearchView(
                isSearchBarVisible = isSearchBarVisible,
                onMenuClick = onMenuClick,
                selectedTheme = theme,
                onThemeChanged = viewModel::updateTheme,
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::updateSearchQuery,
                selectedNotes = selectedNotes,
                onClearSelection = { selectedNotes = emptySet() },
                onPinNotes = { handlePinNotes() },
                onUnPinNotes = { handlePinNotes() },
                onArchiveNotes = {
                    handleAction(
                        { viewModel.archiveNotes(it.toList()) },
                        "${selectedNotes.size} notes archived and unpinned",
                        viewModel::restoreLastArchivedNotes
                    )
                },
                onDeleteNotes = {
                    handleAction(
                        { viewModel.trashNotes(it.toList()) },
                        "${selectedNotes.size} notes moved to trash and unpinned",
                        viewModel::restoreLastDeletedNotes
                    )
                },
                onSearchClosed = {
                    isSearchBarVisible.value = false
                    viewModel.updateSearchQuery("")
                },
                onSetReminder = {
                    showReminderDialog = true
                }
            )
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { tagName ->
                viewModel.addTag(tagName)
                showAddTagDialog = false
            }
        )
    }

    if (showUpdateTagDialog) {
        tagToUpdate?.let { tag ->
            UpdateTagDialog(
                tag = tag,
                onDismiss = { showUpdateTagDialog = false },
                onConfirm = { updatedTag ->
                    viewModel.updateTag(updatedTag)
                    showUpdateTagDialog = false
                },
                onDelete = {
                    viewModel.deleteTag(tag)
                    showUpdateTagDialog = false
                }
            )
        }
    }

    val initialDate = selectedNotes.firstOrNull()?.reminderDate

    ReminderDialog(
        showDialog = showReminderDialog,
        onDismiss = { showReminderDialog = false },
        onSetReminder = { reminderDate ->
            // Update reminder for all selected notes
            selectedNotes.forEach { note ->
                viewModel.updateNoteReminder(
                    noteId = note.id,
                    reminderDate = reminderDate,
                )
            }
            selectedNotes = emptySet() // Clear selection after updating reminder
            showReminderDialog = false
        },
        initialDate = initialDate,
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun ReminderDateDialogPreview() {
    ReminderDialog(
        showDialog = true,
        onDismiss = {},
        onSetReminder = {},
        initialDate = null
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

    val isDateValid = remember(selectedDate) {
        selectedDate >= LocalDate.now()
    }

    val isDateTimeValid = remember(selectedDate, selectedTime) {
        val selectedDateTime = selectedDate.atTime(selectedTime)
        selectedDateTime.atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli() > System.currentTimeMillis()
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerContent(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            .toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(LocalDate.now())
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newSelectedDate =
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(newSelectedDate)
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            showModeToggle = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerContent(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onBack: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    TimePickerDialog(
        onDismiss = onBack,
        onConfirm = {
            val newSelectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
            onTimeSelected(newSelectedTime)
        }
    ) {
        TimePicker(state = timePickerState)
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.height(12.dp))

                content()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Back") }
                    Button(onClick = onConfirm) { Text("Set Reminder") }
                }
            }
        }
    }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    showDialog: Boolean,
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onSetReminder: (Long?) -> Unit
) {
    if (showDialog) {
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

        val isDateValid = remember(selectedDate) {
            selectedDate >= LocalDate.now()
        }

        val isDateTimeValid = remember(selectedDate, selectedTime) {
            val selectedDateTime = selectedDate.atTime(selectedTime)
            selectedDateTime.atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli() > System.currentTimeMillis()
        }


        if (isDatePickerVisible) {
            // Date Picker Dialog
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                    .toEpochMilli(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date =
                            Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        return !date.isBefore(LocalDate.now())
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val newSelectedDate =
                                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                selectedDate = newSelectedDate
                                isDatePickerVisible = false
                            }
                            isDatePickerVisible = false
                        },
                        enabled = isDateValid
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    showModeToggle = false,
                )
            }
        } else {
            // Time Picker Dialog
            val timePickerState = rememberTimePickerState(
                initialHour = selectedTime.hour,
                initialMinute = selectedTime.minute
            )

            TimePickerDialog(
                onDismiss = { isDatePickerVisible = true },  // Go back to Date Picker if dismissed
                onConfirm = {
                    val newSelectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    selectedTime = newSelectedTime
                    val reminderMillis =
                        selectedDate.atTime(selectedTime).atZone(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli()
                    onSetReminder(reminderMillis)
                }
            ) {
                TimePicker(
                    state = timePickerState,
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.height(12.dp))

                content()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Back") }
                    Button(onClick = onConfirm) { Text("Set Reminder") }
                }


            }
        }
    }
}
*/


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


fun LazyStaggeredGridScope.tagHeader(
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagClick: (Long) -> Unit,
    onTagLongClick: (Tag) -> Unit,
    onAddTagClick: () -> Unit
) {

//    if (tags.isNotEmpty()) {   }
    item(span = StaggeredGridItemSpan.FullLine) {
        val scroll = rememberLazyListState()

        LazyRow(
            modifier = Modifier
                .animateItem()
                .wrapContentSize(unbounded = true)
                .width(LocalConfiguration.current.screenWidthDp.dp),
            state = scroll,
            contentPadding = PaddingValues(10.dp, 0.dp, 10.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tags) { tag ->
                TagChip(
                    tag = tag,
                    isSelected = tag.id == selectedTagId,
                    onClick = { onTagClick(tag.id) },
                    onLongClick = { onTagLongClick(tag) }
                )
            }
            item {
                AddTagChip(onClick = onAddTagClick)
            }
        }

    }


}


@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var longPressActive by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            longPressActive = false
            delay(500L)
            longPressActive = true
            onLongClick()

        }
    }

    FilterChip(
        selected = isSelected,
        label = { Text(text = tag.name) },
        modifier = Modifier
            .padding(end = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(tag.color),
            selectedLabelColor = Color.Black,
        ),
        onClick = {
            if (!longPressActive) {
                onClick()
            }
        },
        interactionSource = interactionSource
    )
}


@Composable
fun AddTagChip(onClick: () -> Unit) {
    // Consistent with other chips, but styled differently to indicate 'Add'
    FilterChip(
        selected = false, // 'Add' button shouldn't have a selected state
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Tag",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Add Tag",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        modifier = Modifier.padding(end = 8.dp),
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,  // Matches background of unselected tag
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,   // Matches text color for unselected tag
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ) // Similar border style as unselected tags
    )
}

//data class ColorOption(val color: Color, var isSelected: Boolean = false)


/*@Composable
fun ColorSelector(
    colorOptions: List<ColorOption>,
    onColorSelect: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(colorOptions) { index, colorOption ->
            ColorOption(
                color = colorOption.color,
                isSelected = colorOption.isSelected,
                onClick = { onColorSelect(index) }
            )
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
*/
@Composable
fun HandleOriginalNote(
    originalNote: Note,
    snackBarHostState: SnackbarHostState,
    viewModel: NotesViewModel
) {
    LaunchedEffect(originalNote) {
        if (originalNote.isTrashed || originalNote.isArchived) {
            val message = when {
                originalNote.isTrashed -> "Note moved to trash and unpinned"
                originalNote.isArchived -> "Note archived and unpinned"
                else -> return@LaunchedEffect
            }
            val result = snackBarHostState.showSnackbar(message, "UNDO")
            if (result == SnackbarResult.ActionPerformed) {
                if (originalNote.isTrashed) viewModel.restoreDeletedNote()
                else viewModel.restoreArchivedNotes()
            }
        }
    }
}

