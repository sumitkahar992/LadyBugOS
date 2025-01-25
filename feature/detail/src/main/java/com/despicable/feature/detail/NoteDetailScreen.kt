package com.despicable.feature.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.despicable.core.common.navigation.LocalNavAnimatedVisibilityScope
import com.despicable.core.common.navigation.LocalSharedTransitionScope
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.common.navigation.NoteSharedElementKey
import com.despicable.core.common.navigation.NoteSharedElementType
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.component.ReminderDialog
import com.despicable.core.designsystem.component.ReminderInfo
import com.despicable.core.designsystem.component.TagChip
import com.despicable.core.designsystem.component.rememberContainerColor
import com.despicable.core.designsystem.component.rememberTagColors
import com.despicable.core.model.getRelativeTimeAgo
import com.despicable.feature.detail.components.AddItemButton
import com.despicable.feature.detail.components.ChecklistItem
import com.despicable.feature.detail.components.ColorPickerDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber

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
    var showDeleteDialog by remember { mutableStateOf(false) }
//    val keyboardController = LocalSoftwareKeyboardController.current
//    val focusManager = LocalFocusManager.current


    val disableInTrash = !uiState.isTrashed

    // Don't render anything while loading
    if (uiState.isLoading) {
        return
    }


    val containerColor = rememberContainerColor(uiState.lightColor)

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

    // Handle disabled interactions for trash-related snackBars
    val handleTrashRestore: () -> Unit = {
        scope.launch {
            snackBarHostState.showSnackbar(
                message = "Can't edit in Trash",
                actionLabel = "RESTORE",
                duration = SnackbarDuration.Long,
                withDismissAction = true
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreFromTrash(onComplete = {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = "Note restored",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            ).let { undoResult ->
                                if (undoResult == SnackbarResult.ActionPerformed) {
                                    viewModel.undoRestore()
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    // Show initial snackBar when entering trashed note
    LaunchedEffect(uiState.isTrashed) {
        if (uiState.isTrashed) {
            handleTrashRestore()
        }
    }

    BackHandler {
        Timber.tag("DEBUG").d("[]BackHandler[]")
        onBack()
    }


    // Note Detail screen
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
                .imePadding()
                .skipToLookaheadSize()
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
                .clip(RoundedCornerShape(roundedCornerAnim)),
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            snackbarHost = { SnackbarHost(snackBarHostState) },

            topBar = {
                EditNoteTopAppBar(
                    containerColor = Color.Transparent,
                    isPinned = uiState.isPinned,
                    isArchived = uiState.isArchived,
                    isTrashed = uiState.isTrashed,
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
                    onTogglePin = viewModel::togglePinStatus,
                    onAddReminder = { showReminderDialog = true }
                )
            },
            bottomBar = {
                NoteDetailBottomBar(
//                    modifier = Modifier.imePadding(),
                    onLeftMenuClick = { showLeftBottomSheet = true },
                    onRightMenuClick = { showRightBottomSheet = true },
                    containerColor = containerColor,
                    uiState = uiState
                )
            },
            contentWindowInsets = WindowInsets.ime,
        ) { innerPadding ->

            Box(
                Modifier
                    .fillMaxSize()
                    .horizontalWindowInsetsPadding()
                    .padding(innerPadding)
            ) {
                EditNoteContent(
                    skipModifier = Modifier.skipToLookaheadSize(),
                    modifier = Modifier
                        .fillMaxSize()
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
                    onDisabledClick = handleTrashRestore,
                    enabled = !uiState.isTrashed,
                    content = {
                        // Calculate colors based on theme and noteColor
                        val tagColors = rememberTagColors(uiState.lightColor)

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
                                    enabled = disableInTrash,
                                    isSelected = uiState.selectedTagIds.contains(tag.id),
                                    onClick = { viewModel.toggleTag(tag.id) },
                                    containerColor = tagColors.backgroundColor,
                                    labelColor = tagColors.contentColor
                                )
                            }
                        }
                    }
                )

            }
        }

        // Delete confirmation dialog
        NoteeDialog(
            enabled = showDeleteDialog,
            title = "Delete note ?",
            description = " Are you sure you want to delete this note? This note will be permanently deleted?",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNoteForever(onComplete = onBack)
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )

        // Dialogs
        if (isColorPickerDialogVisible && disableInTrash) {
            ColorPickerDialog(
                selectedColor = containerColor,
                onColorSelected = { selectedColor ->
                    viewModel.updateColor(selectedColor.toArgb())
                    isColorPickerDialogVisible = false
                },
                onDismissRequest = { isColorPickerDialogVisible = false }
            )
        }

        if (showReminderDialog && disableInTrash) {
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
                onDeleteReminder = {
                    viewModel.updateNoteReminder(
                        noteId = uiState.id,
                        reminderDate = null
                    )
                    showReminderDialog = false
                }
            )
        }
    }

    // Bottom Sheets
    if (showLeftBottomSheet && disableInTrash) {
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
                isTrashed = uiState.isTrashed,
                onRestore = {
                    viewModel.restoreFromTrash(onComplete = {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = "Note restored",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            ).let { result ->
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoRestore()
                                }
                            }
                        }
                    })
                },
                onDeleteForever = { showDeleteDialog = true },
                containerColor = containerColor,
                isChecklist = uiState.isCheckList,
                onToggleChecklist = {
                    viewModel.onEvent(CheckListEvent.ToggleChecklist)
                    showRightBottomSheet = false
                }
            )
        }
    }
}

fun Modifier.horizontalWindowInsetsPadding() = composed {
    val layoutDirection = LocalLayoutDirection.current
    this
        .padding(
            start = WindowInsets.safeDrawing
                .asPaddingValues()
                .calculateLeftPadding(layoutDirection)
        )
        .padding(
            end = WindowInsets.safeDrawing
                .asPaddingValues()
                .calculateRightPadding(layoutDirection)
        )
}


@Composable
fun EditNoteContent(
    modifier: Modifier = Modifier,
    viewModel: NoteDetailViewModel = koinViewModel(),
    skipModifier: Modifier = Modifier,
    uiState: NoteUiState,
    onTitleChange: (TextFieldValue) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onOpenColorPicker: () -> Unit,
    onClickReminderInfo: () -> Unit,
    isDone: Boolean,
    onDisabledClick: () -> Unit = {},
    enabled: Boolean,
    content: @Composable () -> Unit = {}
) {
//    val sharedTransitionScope =
//        LocalSharedTransitionScope.current
//            ?: throw IllegalStateException("No scope found")
    val contentFocusRequester = remember { FocusRequester() }

    val list = uiState.checklistItems
    val lazyListState = rememberLazyListState()

    /*  val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
          Timber.tag("DEBUG").d("reorderableState: from=${from.index}, to=${to.index}")
          viewModel.onEvent(CheckListEvent.ReorderChecklistItems(from.index, to.index))
      }*/

    // Modify the reorderable state to account for header offset
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            // Only allow reordering if we're not touching the header (index 0)
            if (from.index > 3 && to.index > 3) {
                // Adjust indices to account for header
                val fromIndex = from.index - 4
                val toIndex = to.index - 4
                viewModel.onEvent(CheckListEvent.ReorderChecklistItems(fromIndex, toIndex))
            }
        }
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current


    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp) // Default spacing between items
    ) {
        item("Label") { content() }

        item("Spacer") { Spacer(modifier = Modifier.height(6.dp)) } // Reduced from 16.dp

        // Title section
        item("Title") {
            NoteTitleSection(
                modifier = skipModifier,
                uiState = uiState,
                onTitleChange = onTitleChange,
                onOpenColorPicker = onOpenColorPicker,
                enabled = enabled,
                onDisabledClick = onDisabledClick,
                keyboardActions = KeyboardActions(
                    onNext = {
                        when {
                            uiState.isCheckList && list.isEmpty() -> {
                                // When checklist is empty, add first item and focus it
                                viewModel.onEvent(CheckListEvent.AddChecklistItemAt(0))
                            }

                            uiState.isCheckList -> {
                                // Focus first item if checklist exists
                                viewModel.onEvent(CheckListEvent.UpdateFocusedPosition(0))
                            }

                            else -> {
                                // Normal content focus for non-checklist
                                contentFocusRequester.requestFocus()
                            }
                        }

                        // Move cursor to end of content
                        onContentChange(
                            uiState.contentFieldValue.copy(
                                selection = TextRange(uiState.contentFieldValue.text.length)
                            )
                        )
                    }
                )
            )
        }

        item("Spacer2") { Spacer(modifier = Modifier.height(8.dp)) }

        // Content section

        Timber.tag("DEBUG").d("[(uiState.isCheckList]-[${uiState.isCheckList}]")


        if (uiState.isCheckList) {

            itemsIndexed(
                items = list,
                key = { _, item -> item.id }
            ) { index, item ->

                val shouldFocus = index == uiState.focusedItemPosition
                val itemFocusRequester = remember(item.id) { FocusRequester() }


                ReorderableItem(
                    state = reorderableState,
                    key = item.id
                ) { isDragging ->

                    ChecklistItem(
                        dragModifier = Modifier
                            .draggableHandle(
                                onDragStarted = {
                                    focusManager.clearFocus()
                                    viewModel.onEvent(CheckListEvent.UpdateFocusedPosition(-1))
                                },
                                onDragStopped = {}
                            ),
                        item = item,
                        onCheckedChange = {
                            viewModel.onEvent(CheckListEvent.ChecklistItemChecked(item))
                        },
                        onContentChange = { content ->
                            viewModel.onEvent(
                                CheckListEvent.UpdateChecklistItemContent(
                                    item,
                                    content
                                )
                            )
                        },
                        onDelete = {
                            viewModel.onEvent(CheckListEvent.RemoveChecklistItem(index))
                        },
                        onNext = {
                            viewModel.onEvent(CheckListEvent.AddChecklistItemAt(index + 1))
                        },
                        focusRequester = itemFocusRequester,
                        shouldFocus = shouldFocus,
                        isDragging = isDragging,
                        onFocusChange = { focused ->
                            if (focused) {
                                keyboardController?.show()
                            }
                        }
                    )
                }

                LaunchedEffect(list.isEmpty()) {
                    if (list.isEmpty()) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
            }

            item {
                AddItemButton(
                    onAddClick = {
                        viewModel.onEvent(CheckListEvent.AddChecklistItemAt(list.size))
                    }
                )
            }

        } else {
            item("Content") {
                NoteContentSection(
                    modifier = skipModifier.focusRequester(contentFocusRequester),
                    uiState = uiState,
                    onContentChange = onContentChange,
                    enabled = enabled,
                    onDisabledClick = onDisabledClick,
                )
            }
        }


        // Reminder section
        uiState.reminderDate?.let { date ->
            item("Reminder") {
                Spacer(modifier = Modifier.height(8.dp))
                ReminderInfo(
                    reminderDate = date,
                    isDone = isDone,
                    onClick = onClickReminderInfo,
                    isClickable = true,
                    modifier = skipModifier
                        .padding(horizontal = 8.dp)
                        .animateItem(
                            fadeInSpec = null, fadeOutSpec = null
                        ),
                )
            }
        }

        item("Spacer3") { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

sealed class NoteFieldType(
    val placeholder: String,
    val maxLines: Int,
    val textStyle: @Composable () -> TextStyle,
    val keyboardOptions: KeyboardOptions,
    val minHeight: @Composable () -> Int = { 0 },
    val maxHeight: @Composable () -> Int? = { null }
) {
    data object Title : NoteFieldType(
        placeholder = "Title",
        maxLines = 2,
        textStyle = {
            MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        minHeight = { with(LocalDensity.current) { 28.sp.toDp().value.toInt() } },
        maxHeight = { with(LocalDensity.current) { (28.sp * 2.5f).toDp().value.toInt() } }
    )

    data object Content : NoteFieldType(
        placeholder = "Content",
        maxLines = Int.MAX_VALUE,
        textStyle = {
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        ),
        minHeight = { 250 }
    )
}

@Composable
fun NoteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    type: NoteFieldType,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDisabledClick: () -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val clickableModifier = if (!enabled) {
        Modifier.clickable(onClick = onDisabledClick)
    } else {
        Modifier
    }

    val baseModifier = modifier
        .heightIn(
            min = type.minHeight().dp,
            max = type.maxHeight()?.dp ?: Int.MAX_VALUE.dp
        )
        .then(clickableModifier)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = baseModifier,
        enabled = enabled,
        textStyle = type.textStyle(),
        maxLines = type.maxLines,
        keyboardActions = keyboardActions,
        keyboardOptions = type.keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (value.text.isEmpty()) {
                Text(
                    text = type.placeholder,
                    style = type.textStyle().copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
            innerTextField()
        }
    )
}

@Composable
fun NoteTitleSection(
    uiState: NoteUiState,
    onTitleChange: (TextFieldValue) -> Unit,
    onOpenColorPicker: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onDisabledClick: () -> Unit,
    keyboardActions: KeyboardActions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoteTextField(
            value = uiState.titleFieldValue,
            onValueChange = onTitleChange,
            type = NoteFieldType.Title,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onDisabledClick = onDisabledClick,
            keyboardActions = keyboardActions
        )

        IconButton(
            enabled = enabled,
            onClick = onOpenColorPicker,
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = "Open color picker"
            )
        }
    }
}

@Composable
fun NoteContentSection(
    uiState: NoteUiState,
    onContentChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onDisabledClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)  // Match title section padding
    ) {

        NoteTextField(
            value = uiState.contentFieldValue,
            onValueChange = onContentChange,
            type = NoteFieldType.Content,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onDisabledClick = onDisabledClick,
        )
    }
}

/*
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
                        },
                        enabled = enabled,
                        onDisabledClick = onDisabledClick
                    )

                    IconButton(
                        enabled = enabled,
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
                    useBasicTextField = false,
                    enabled = enabled,
                    onDisabledClick = onDisabledClick
                )
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
    useBasicTextField: Boolean = false,
    enabled: Boolean = true,
    onDisabledClick: () -> Unit = {}
) {
    val clickableModifier = if (!enabled) {
        Modifier.clickable(onClick = onDisabledClick)
    } else {
        Modifier
    }

    if (useBasicTextField) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.then(clickableModifier),
            textStyle = textStyle,
            maxLines = maxLines,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            onTextLayout = onTextLayout,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
            modifier = modifier.then(clickableModifier),
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
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
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent  // Remove the line
            )
        )
    }
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteTopAppBar(
    containerColor: Color,
    isPinned: Boolean,
    isArchived: Boolean,
    isTrashed: Boolean,
    onBack: () -> Unit,
    onAddReminder: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onTogglePin: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {},
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
            if (!isTrashed) {

                // Pin / Unpin action
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin note" else "Pin note"
                    )
                }

                // Add Reminder action
                IconButton(onClick = onAddReminder) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationAdd,
                        contentDescription = "Add reminder"
                    )
                }

                // Archive / Unarchive action
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
        title = {  },
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
//    modifier: Modifier = Modifier,
    onLeftMenuClick: () -> Unit,
    onRightMenuClick: () -> Unit,
    containerColor: Color,
    uiState: NoteUiState,
) {
    val barHeight = 54.dp

    BottomAppBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .horizontalWindowInsetsPadding()
            .height(barHeight),
        contentPadding = PaddingValues(5.dp),
        containerColor = containerColor,
        tonalElevation = 0.dp,
//        windowInsets = BottomAppBarDefaults.windowInsets.union(WindowInsets.ime),
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
    isTrashed: Boolean,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    isChecklist: Boolean,
    onToggleChecklist: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val (icon, text) = if (isChecklist) {
        Icons.AutoMirrored.Outlined.Segment to "Content"
    } else {
        Icons.Outlined.CheckBox to "Checkboxes"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(vertical = 16.dp)
    ) {
        if (isTrashed) {
            BottomSheetItem(
                icon = Icons.Outlined.Restore,
                text = "Restore note",
                onClick = onRestore
            )
            BottomSheetItem(
                icon = Icons.Outlined.DeleteForever,
                text = "Delete forever",
                onClick = onDeleteForever
            )
        } else {
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
            BottomSheetItem(
                icon = icon,
                text = text,
                onClick = onToggleChecklist
            )
        }
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