package com.example.ladybugos.ui.drawer.home

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ladybugos.model.Tag
import com.example.ladybugos.ui.theme.GridLayout
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId


@Composable
fun CreateNewLabelScreen(
    viewModel: NoteListViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()

    TagManagementScreen(
        tags = uiState.tags,
        onNavigateBack = onNavigateBack,
        onAddTag = { tagName -> viewModel.addTag(tagName) },
        onUpdateTag = { tag -> viewModel.updateTag(tag) },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) }
    )
}


@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun TagManagementScreen(
    tags: List<Tag>,
    onNavigateBack: () -> Unit,
    onAddTag: (String) -> Unit,
    onUpdateTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit
) {
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Tag?>(null) }
    var newTagName by remember { mutableStateOf("") }
    var isCreatingNewTag by remember { mutableStateOf(false) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Sort tags to show newest first
    val sortedTags = remember(tags) {
        tags.sortedByDescending { it.id }
    }

    fun clearFocusAndHideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
        isCreatingNewTag = false
        editingTag = null
        newTagName = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Show error snackBar
        AnimatedVisibility(
            visible = showErrorSnackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp) // Positioned below TopAppBar
                .zIndex(1f), // Ensure it appears above other content
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp),
                action = { },
                dismissAction = {
                    IconButton(onClick = { showErrorSnackbar = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit labels") },
                    navigationIcon = {
                        IconButton(onClick = {
                            clearFocusAndHideKeyboard()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Top divider
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "create_new") {
                        TagListItem(
                            tag = Tag(name = ""),
                            isEditing = isCreatingNewTag,
                            isNewTag = true,
                            newTagName = newTagName,
                            onNewTagNameChange = { newTagName = it },
                            onEditClick = {
                                editingTag = null
                                isCreatingNewTag = true
                                keyboardController?.show()
                            },
                            onDeleteClick = {
                                clearFocusAndHideKeyboard()
                            },
                            onEditComplete = { tagName ->
                                if (tagName.isNotBlank()) {
                                    if (sortedTags.any {
                                            it.name.equals(
                                                tagName,
                                                ignoreCase = false
                                            )
                                        }) {
                                        errorMessage = "Tag already exists"
                                        showErrorSnackbar = true
                                    } else {
                                        onAddTag(tagName)
                                        clearFocusAndHideKeyboard()
                                    }
                                }
                            }
                        )
                    }

                    items(
                        items = sortedTags,
                        key = { it.id }
                    ) { tag ->
                        TagListItem(
                            modifier = Modifier.animateItem(
                                fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )
                            ),
                            tag = tag,
                            isEditing = editingTag?.id == tag.id,
                            isNewTag = false,
                            newTagName = "",
                            onNewTagNameChange = {},
                            onEditClick = {
                                isCreatingNewTag = false
                                newTagName = ""
                                editingTag = tag
                            },
                            onDeleteClick = { showDeleteDialog = tag },
                            onEditComplete = { newName ->
                                if (sortedTags.any {
                                        it.id != tag.id && it.name.equals(
                                            newName,
                                            ignoreCase = true
                                        )
                                    }) {
                                    errorMessage = "Tag already exists"
                                    showErrorSnackbar = true
                                } else {
                                    onUpdateTag(tag.copy(name = newName))
                                    clearFocusAndHideKeyboard()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { tag ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete label?") },
                text = {
                    Text("We'll delete this label and remove it from all of your Keep notes. Your notes won't be deleted.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteTag(tag)
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Auto-hide snackbar after delay
        LaunchedEffect(showErrorSnackbar) {
            delay(2000)
            showErrorSnackbar = false
        }
    }
}

@Composable
private fun TagListItem(
    modifier: Modifier = Modifier,
    tag: Tag,
    isEditing: Boolean,
    isNewTag: Boolean,
    newTagName: String,
    onNewTagNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditComplete: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var fieldValue by remember(tag.id, isEditing, newTagName, tag.name) {
        mutableStateOf(
            TextFieldValue(
                text = when {
                    isNewTag -> newTagName
                    isEditing -> tag.name
                    else -> tag.name
                },
                selection = TextRange(
                    when {
                        isNewTag -> newTagName.length
                        isEditing -> tag.name.length
                        else -> 0
                    }
                )
            )
        )
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    AnimatedContent(
        targetState = isEditing,
        modifier = modifier,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }, label = ""
    ) { editing ->
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !editing) { onEditClick() }
                    .background(
                        color = if (editing)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .weight(0.2f)
                        .clickable(enabled = editing) {
                            onDeleteClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Left Icon
                    Icon(
                        imageVector = when {
                            isNewTag && editing -> Icons.Default.Close
                            isNewTag -> Icons.Default.Add
                            editing -> Icons.Outlined.Delete
                            else -> Icons.AutoMirrored.Outlined.Label
                        },
                        contentDescription = null,
//                        tint = if (editing && !isNewTag) MaterialTheme.colorScheme.error
//                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }


                // Text Field / Label
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { value ->
                            fieldValue = value
                            if (isNewTag) {
                                onNewTagNameChange(value.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        enabled = editing,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                if (fieldValue.text.isEmpty() && isNewTag) {
                                    Text(
                                        "Create New Label",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onEditComplete(fieldValue.text)
                            }
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .weight(0.2f),
                    contentAlignment = Alignment.Center
                ) {
                    // Right Icon
                    if ((isNewTag && fieldValue.text.isNotBlank()) || editing) {
                        IconButton(
                            onClick = { onEditComplete(fieldValue.text) }
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Save",
                            )
                        }
                    } else if (!isNewTag) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }
    }
}


@Composable
fun LayoutSelectionDialog(
    currentLayout: GridLayout,
    onLayoutSelected: (GridLayout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Layout") },
        text = {
            Column {
                GridLayout.entries.forEach { layout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLayoutSelected(layout) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = layout == currentLayout,
                            onClick = { onLayoutSelected(layout) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (layout) {
                                GridLayout.OneColumn -> "Single Column"
                                GridLayout.TwoColumns -> "Two Columns"
                                GridLayout.ThreeColumns -> "Three Columns"
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            when (layout) {
                                GridLayout.OneColumn -> Icon(
                                    Icons.Default.ViewStream,
                                    contentDescription = null
                                )

                                GridLayout.TwoColumns -> Icon(
                                    Icons.Default.ViewModule,
                                    contentDescription = null
                                )

                                GridLayout.ThreeColumns -> Icon(
                                    Icons.Default.ViewComfy,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
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

    /*    val isDateValid = remember(selectedDate) {
            selectedDate >= LocalDate.now()
        }

        val isDateTimeValid = remember(selectedDate, selectedTime) {
            val selectedDateTime = selectedDate.atTime(selectedTime)
            selectedDateTime.atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli() > System.currentTimeMillis()
        }*/

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

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun TagManagementScreenPreview() {
    val sampleTags = listOf(
        Tag(name = "Work"),
        Tag(name = "Personal"),
        Tag(name = "Shopping"),
        Tag(name = "Ideas")
    )

    MaterialTheme {
        TagManagementScreen(
            tags = sampleTags,
            onNavigateBack = {},
            onAddTag = {},
            onUpdateTag = {},
            onDeleteTag = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagListItemPreview() {
    val tag = Tag(name = "Sample Tag")

    MaterialTheme {
        Column {
            // Normal state
            TagListItem(
                tag = tag,
                isEditing = false,
                isNewTag = false,
                newTagName = "",
                onNewTagNameChange = {},
                onEditClick = {},
                onDeleteClick = {},
                onEditComplete = {}
            )

            // Editing state
            TagListItem(
                tag = tag,
                isEditing = true,
                isNewTag = false,
                newTagName = "",
                onNewTagNameChange = {},
                onEditClick = {},
                onDeleteClick = {},
                onEditComplete = {}
            )
        }
    }
}

/*
@Composable
fun TagManagementDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onUpdateTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(300.dp), // Fixed width for better UI
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = null, // Removed title as requested
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // New Label Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // Center the content
                ) {
                    var newTagName by remember { mutableStateOf("") }

                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("Create new label") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        trailingIcon = if (newTagName.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = {
                                        onAddTag(newTagName)
                                        newTagName = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Check, "Add label")
                                }
                            }
                        } else {
                            {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, "Close")
                                }
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Tags List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        TagListItem(tag = tag, onEditTag = onUpdateTag, onDeleteTag = {
                            onDeleteTag(
                                tag
                            )
                        })
                    }
                }
            }
        },
        confirmButton = {} // Removed the confirm button
    )
}

@Composable
private fun TagListItem(
    tag: Tag,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(tag.id) { mutableStateOf(tag.name) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isEditing = true }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center // Center the content
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (isEditing) {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = {
                                if (editedName.isNotBlank()) {
                                    onEditTag(tag.copy(name = editedName))
                                }
                                isEditing = false
                            }
                        ) {
                            Icon(Icons.Default.Check, "Save")
                        }
                        IconButton(onClick = onDeleteTag) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        } else {
            Text(
                text = tag.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { isEditing = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
 */

/*

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

*/

fun Modifier.animateItemPlacement(): Modifier = composed {
    this.then(
        Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    )
}
