package com.example.ladybugos.ui.drawer.note_detail

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ladybugos.model.darken
import com.example.ladybugos.model.getRelativeTimeAgo
import com.example.ladybugos.navigation.LocalNavAnimatedVisibilityScope
import com.example.ladybugos.navigation.LocalSharedTransitionScope
import com.example.ladybugos.navigation.NoteAction
import com.example.ladybugos.navigation.NoteSharedElementKey
import com.example.ladybugos.navigation.NoteSharedElementType
import com.example.ladybugos.ui.components.ReminderInfo
import com.example.ladybugos.ui.components.TagChip
import com.example.ladybugos.ui.drawer.home.ReminderDialog
import com.example.ladybugos.ui.theme.LocalThemeProvider
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
    onDelete: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onUnArchive: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    var isColorPickerDialogVisible by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showLeftBottomSheet by remember { mutableStateOf(false) }
    var showRightBottomSheet by remember { mutableStateOf(false) }
    val leftBottomSheetState = rememberModalBottomSheetState()
    val rightBottomSheetState = rememberModalBottomSheetState()
    val noteId = uiState.id
    val scope = rememberCoroutineScope()

    // Don't render anything while loading
    if (uiState.isLoading) {
        return
    }

    // Calculate container color based on theme
    val isDarkTheme = LocalThemeProvider.isDarkTheme

    val containerColor = when {
        uiState.lightColor == 0 -> MaterialTheme.colorScheme.surface
        isDarkTheme -> remember(uiState.lightColor) { Color(uiState.lightColor).darken(0.4f) }
        else -> remember(uiState.lightColor) { Color(uiState.lightColor) }
    }

    // Handler for note actions
    val handleAction: (NoteAction) -> Unit = { action ->
        scope.launch {
            viewModel.handleNoteAction(action) {
                when (action) {
                    is NoteAction.Delete -> onDelete(action.noteId)
                    is NoteAction.Archive -> onArchive(action.noteId)
                    is NoteAction.Unarchive -> onUnArchive(action.noteId)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.deleteNoteIfEmpty()
            viewModel.saveNote(
                onComplete = onBack,
                onSkip = onBack
            )
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No Scope found")
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
        ?: throw IllegalStateException("No Scope found")

    // Single corner animation for consistency
    val roundedCornerAnim by animatedVisibilityScope.transition.animateDp(label = "Rounded corner") {
        if (it != EnterExitState.Visible) 12.dp else 0.dp
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(
                        key = NoteSharedElementKey(
                            noteId = uiState.id,
                            type = NoteSharedElementType.Bounds
                        )
                    ),
                    animatedVisibilityScope,
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(roundedCornerAnim)
                    ),
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                )
                .clip(RoundedCornerShape(roundedCornerAnim))
                .imePadding(),
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            snackbarHost = { SnackbarHost(snackBarHostState) },
            topBar = {
                EditNoteTopAppBar(
                    containerColor = Color.Transparent,
                    isPinned = uiState.isPinned,
                    isArchived = uiState.isArchived,
                    onBack = {
                        viewModel.saveNote(
                            onComplete = onBack,
                            onSkip = onBack
                        )
                    },
                    onDelete = {
                        handleAction(NoteAction.Delete(noteId))
                    },
                    onArchive = {
                        handleAction(NoteAction.Archive(noteId))

                    },
                    onUnarchive = {
                        handleAction(NoteAction.Unarchive(noteId))
                    },
                    onTogglePin = viewModel::togglePinStatus
                )
            },
            bottomBar = {
                NoteDetailBottomBar(
                    onLeftMenuClick = { showLeftBottomSheet = true },
                    onRightMenuClick = { showRightBottomSheet = true },
                    containerColor = containerColor,
                    uiState = uiState
                )
            }
        ) { innerPadding ->
            EditNoteContent(
                modifier = Modifier
                    .padding(innerPadding)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = NoteSharedElementKey(
                                uiState.id,
                                NoteSharedElementType.Content
                            )
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(
                            RoundedCornerShape(roundedCornerAnim)
                        ),
                    ),
                uiState = uiState,
                onTitleChange = viewModel::updateNoteTitle,
                onContentChange = viewModel::updateNoteContent,
                onOpenColorPicker = { isColorPickerDialogVisible = true },
                onClickReminderInfo = { showReminderDialog = true },
                isDone = uiState.isDone,
                onRemoveReminder = {
                    viewModel.updateNoteReminder(
                        noteId = uiState.id,
                        reminderDate = null
                    )
                },
                content = {

                    val selectedContainerColor =
                        if (isDarkTheme) Color.White.copy(alpha = 0.15f) // Semi-transparent white for dark theme
                        else Color.White.copy(alpha = 0.85f) // More opaque white for light theme


                    val selectedLabelColor =
                        if (isDarkTheme) Color.White.copy(alpha = 0.87f)
                        else Color.Black.copy(alpha = 0.87f)


                    LazyRow(
                        modifier = Modifier
                            .wrapContentSize(unbounded = true)
                            .width(LocalConfiguration.current.screenWidthDp.dp),
                        contentPadding = PaddingValues(16.dp, 0.dp, 10.dp, 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.allTags) { tag ->
                            TagChip(
                                tag = tag,
                                isSelected = uiState.selectedTagIds.contains(tag.id),
                                onClick = { viewModel.toggleTag(tag.id) },
                                containerColor = selectedContainerColor,
                                labelColor = selectedLabelColor
                            )
                        }
                    }
                }
            )

        }

        // Dialogs
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
                }
            )
        }
    }

    // Bottom Sheets
    if (showLeftBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLeftBottomSheet = false },
            sheetState = leftBottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = containerColor
        ) {
            LeftBottomSheetContent(
                containerColor = containerColor
            )
        }
    }

    if (showRightBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRightBottomSheet = false },
            sheetState = rightBottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = containerColor
        ) {
            RightBottomSheetContent(
                containerColor = containerColor
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EditNoteContent(
    modifier: Modifier = Modifier,
    uiState: NoteUiState,
    onTitleChange: (TextFieldValue) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onOpenColorPicker: () -> Unit,
    onClickReminderInfo: () -> Unit,
    isDone: Boolean,
    onRemoveReminder: () -> Unit,
    content: @Composable () -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No scope found")

    with(sharedTransitionScope) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                top = 0.dp,
                bottom = 60.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp) // Default spacing between items
        ) {
            item { content() }

            item { Spacer(modifier = Modifier.height(6.dp)) } // Reduced from 16.dp

            // Title section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var titleHeight by remember { mutableIntStateOf(0) }

                    NoteTextField(
                        value = uiState.titleFieldValue,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(
                                min = with(LocalDensity.current) { 28.sp.toDp() },
                                max = with(LocalDensity.current) { (28.sp * 2.5f).toDp() }
                            ),
                        placeholder = "Title",
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        useBasicTextField = true,
                        onTextLayout = { layoutResult ->
                            // Update height based on text layout
                            titleHeight = layoutResult.size.height
                        }
                    )

                    IconButton(
                        onClick = onOpenColorPicker,
                        modifier = Modifier.skipToLookaheadSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Open color picker"
                        )
                    }
                }
            }

            // Content section
            item {
                NoteTextField(
                    value = uiState.contentFieldValue,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp),
                    placeholder = "Content",
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = 0.15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    ),
                    maxLines = Int.MAX_VALUE,
                    singleLine = false,
                    useBasicTextField = false
                )
            }

            // Reminder section
            uiState.reminderDate?.let { date ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ReminderInfo(
                        reminderDate = date,
                        isDone = isDone,
                        onClick = onClickReminderInfo,
                        isClickable = true,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(
                                fadeInSpec = null, fadeOutSpec = null
                            ),
                        onRemoveReminder = onRemoveReminder
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun NoteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    useBasicTextField: Boolean = false
) {
    if (useBasicTextField) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            textStyle = textStyle,
            maxLines = maxLines,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            onTextLayout = onTextLayout,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp) // Match TextField's internal padding
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    } else {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle,
            placeholder = if (value.text.isEmpty()) {
                { Text(placeholder) }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteTopAppBar(
    containerColor: Color,
    isPinned: Boolean,
    isArchived: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onTogglePin: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { /* Empty title */ },
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
            // Pin action
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (isPinned) "Unpin note" else "Pin note"
                )
            }

            // Archive/Unarchive action based on current state
            IconButton(
                onClick = if (isArchived) onUnarchive else onArchive
            ) {
                Icon(
                    imageVector = if (isArchived) Icons.Filled.Unarchive else Icons.Outlined.Archive,
                    contentDescription = if (isArchived) "Unarchive note" else "Archive note"
                )
            }

            // Delete action
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete note"
                )
            }
        }
    )
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteTopAppBar(
    containerColor: Color,
    screenType: NoteScreenType,
    isPinned: Boolean,
    isArchived: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onTogglePin: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { *//* Empty title *//* },
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
            when (screenType) {
                NoteScreenType.LIST -> {
                    // Pin action
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin note" else "Pin note"
                        )
                    }
                    // Archive action
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = "Archive note"
                        )
                    }
                }
                NoteScreenType.REMINDER -> {
                    // Pin action
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin note" else "Pin note"
                        )
                    }
                    // Archive/Unarchive based on current state
                    IconButton(
                        onClick = if (isArchived) onUnarchive else onArchive
                    ) {
                        Icon(
                            imageVector = if (isArchived) {
                                Icons.Filled.Unarchive
                            } else {
                                Icons.Outlined.Archive
                            },
                            contentDescription = if (isArchived) {
                                "Unarchive note"
                            } else {
                                "Archive note"
                            }
                        )
                    }
                }
                NoteScreenType.ARCHIVE -> {
                    // Pin action
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin note" else "Pin note"
                        )
                    }
                    // Unarchive action
                    IconButton(onClick = onUnarchive) {
                        Icon(
                            imageVector = Icons.Filled.Unarchive,
                            contentDescription = "Unarchive note"
                        )
                    }
                }
            }

            // Delete action always visible
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete note"
                )
            }
        }
    )
}*/

@Composable
private fun NoteDetailBottomBar(
    onLeftMenuClick: () -> Unit,
    onRightMenuClick: () -> Unit,
    containerColor: Color,
    uiState: NoteUiState
) {
    BottomAppBar(
        contentPadding = PaddingValues(5.dp),
        containerColor = containerColor,
        modifier = Modifier.height(68.dp),
        tonalElevation = 0.dp,
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLeftMenuClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Left menu",
                    )
                }
                Text(
                    text = "Edited ${getRelativeTimeAgo(uiState.updateDate)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    fontSize = 12.sp
                )
                IconButton(
                    onClick = onRightMenuClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Right menu",
                    )
                }
            }

        }
    )
}

@Composable
private fun LeftBottomSheetContent(
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(vertical = 16.dp)
    ) {
        BottomSheetItem(
            icon = Icons.Default.Delete,
            text = "Delete",
            onClick = {

            }
        )
        BottomSheetItem(
            icon = Icons.Default.ContentCopy,
            text = "Make a copy",
            onClick = { /* Handle copy */ }
        )
        BottomSheetItem(
            icon = Icons.Default.Share,
            text = "Send",
            onClick = { /* Handle send */ }
        )
        BottomSheetItem(
            icon = Icons.Default.Person,
            text = "Collaborator",
            onClick = { /* Handle collaborator */ }
        )
        BottomSheetItem(
            icon = Icons.AutoMirrored.Filled.Label,
            text = "Labels",
            onClick = { /* Handle labels */ }
        )
        BottomSheetItem(
            icon = Icons.AutoMirrored.Filled.Help,
            text = "Help & feedback",
            onClick = { /* Handle help */ }
        )
    }
}

@Composable
private fun RightBottomSheetContent(
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(vertical = 16.dp)
    ) {
        BottomSheetItem(
            icon = Icons.Default.PhotoCamera,
            text = "Take photo",
            onClick = { /* Handle take photo */ }
        )
        BottomSheetItem(
            icon = Icons.Default.Image,
            text = "Add image",
            onClick = { /* Handle add image */ }
        )
        BottomSheetItem(
            icon = Icons.Default.Draw,
            text = "Drawing",
            onClick = { /* Handle drawing */ }
        )
        BottomSheetItem(
            icon = Icons.Default.KeyboardVoice,
            text = "Recording",
            onClick = { /* Handle recording */ }
        )
    }
}

@Composable
private fun BottomSheetItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}