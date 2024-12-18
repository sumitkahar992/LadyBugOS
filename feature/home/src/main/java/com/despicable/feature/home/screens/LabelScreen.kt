package com.despicable.feature.home.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import com.despicable.feature.home.NoteListViewModel
import com.despicable.core.model.Tag
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun LabelScreen(
    viewModel: NoteListViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()

    TagManagementScreen(
        tags = uiState.tagState.availableTags,
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
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
            modifier = Modifier.imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Edit labels", Modifier.padding(start = 16.dp)) },
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
                            onDeleteClick = {
                                tagToDelete = tag
                                showDeleteDialog = true
                            },
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
        tagToDelete?.let { tag ->
            NoteeDialog(
                enabled = showDeleteDialog,
                onDismiss = {
                    showDeleteDialog = false
                    tagToDelete = null
                },
                title = "Delete label?",
                description = "We'll delete this label and remove it from all of your notes. Your notes won't be deleted.",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = {
                    onDeleteTag(tag)
                    showDeleteDialog = false
                    tagToDelete = null
                }
            )
        }

        // Auto-hide snackBar after delay
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
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Left Icon
                IconButton(
                    onClick = onDeleteClick,
                    enabled = editing
                ) {
                    Icon(
                        imageVector = when {
                            isNewTag && editing -> Icons.Default.Close
                            isNewTag -> Icons.Default.Add
                            editing -> Icons.Outlined.Delete
                            else -> Icons.AutoMirrored.Outlined.Label
                        },
                        contentDescription = null,
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


                // Right Icon
                if (isNewTag || editing) {
                    IconButton(
                        onClick = { onEditComplete(fieldValue.text) }
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Save",
                        )
                    }
                } else {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
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


@PreviewLightDark
@Composable
fun TagManagementScreenPreview() {
    // Sample tags with realistic names and sequential IDs
    val sampleTags = listOf(
        Tag(id = 10, name = "Work"),
        Tag(id = 9, name = "Personal"),
        Tag(id = 8, name = "Shopping"),
        Tag(id = 7, name = "Ideas"),
        Tag(id = 6, name = "Travel"),
        Tag(id = 5, name = "Projects"),
        Tag(id = 4, name = "Family"),
        Tag(id = 3, name = "Health"),
        Tag(id = 2, name = "Books"),
        Tag(id = 1, name = "Recipes")
    )
    LadyBugOSTheme {
        TagManagementScreen(
            tags = sampleTags,
            onNavigateBack = {},
            onAddTag = {},
            onUpdateTag = {},
            onDeleteTag = {}
        )
    }
}
