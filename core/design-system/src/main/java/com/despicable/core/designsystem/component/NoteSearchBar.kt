package com.despicable.core.designsystem.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.despicable.core.model.Note

sealed class ScreenType {
    data object List : ScreenType()
    data object Archive : ScreenType()
    data object Trash : ScreenType()
}

@Composable
fun ExpandedSearchView(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    isFocused: MutableState<Boolean>,
    onSearchActiveChange: (Boolean) -> Unit,
    focusManager: FocusManager
) {

    // Use LaunchedEffect with a specific condition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text("Search your notes", color = Color.Gray) },
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 44.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused.value = it.isFocused },
        interactionSource = remember { MutableInteractionSource() },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Search"
                    )
                }
            }
        },
        leadingIcon = {
            IconButton(onClick = {
                onSearchActiveChange(false)
                onSearchQueryChanged("")
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                isFocused.value = false
            }
        ),
        singleLine = true
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    onClearSelection: () -> Unit,
    onPinNotes: () -> Unit = {},
    onUnpinNotes: () -> Unit = {},
    onSetReminder: () -> Unit = {},
    onArchiveNotes: () -> Unit = {},
    onUnarchiveNotes: () -> Unit = {},
    onRestoreNotes: () -> Unit = {},
    onDeleteNotes: () -> Unit = {},
    selectedNotes: Set<Note>,
    screenType: ScreenType
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            },
        tonalElevation = 4.dp
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            ),
            modifier = modifier,
            title = { Text("${selectedNotes.size} selected") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            },
            actions = {
                when (screenType) {
                    ScreenType.List -> {
                        PinUnpinIcon(selectedNotes, onPinNotes, onUnpinNotes)
                        IconButton(onClick = onSetReminder) {
                            Icon(
                                Icons.Outlined.NotificationAdd,
                                contentDescription = "Reminder notes"
                            )
                        }
                        IconButton(onClick = onArchiveNotes) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archive notes")
                        }

                        IconButton(onClick = onDeleteNotes) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete notes")
                        }
                    }

                    ScreenType.Archive -> {
                        PinUnpinIcon(selectedNotes, onPinNotes, onUnpinNotes)

                        IconButton(onClick = onSetReminder) {
                            Icon(
                                Icons.Outlined.NotificationAdd,
                                contentDescription = "Reminder notes"
                            )
                        }
                        IconButton(onClick = onUnarchiveNotes) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Unarchive notes")
                        }
                        IconButton(onClick = onDeleteNotes) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete notes")
                        }
                    }

                    ScreenType.Trash -> {
                        IconButton(onClick = onRestoreNotes) {
                            Icon(Icons.Filled.Restore, contentDescription = "Restore notes")
                        }
                        IconButton(onClick = onDeleteNotes) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete notes")
                        }
                    }
                }
            },
            scrollBehavior = null
        )
    }
}

@Composable
private fun PinUnpinIcon(
    selectedNotes: Set<Note>,
    onPinNotes: () -> Unit,
    onUnpinNotes: () -> Unit
) {
    IconButton(onClick = if (selectedNotes.all { it.isPinned }) onUnpinNotes else onPinNotes) {
        Icon(
            imageVector = if (selectedNotes.all { it.isPinned }) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (selectedNotes.all { it.isPinned }) "Unpin notes" else "Pin notes"
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedSearchView(
    modifier: Modifier = Modifier,
    title: String = "",
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onGridLayoutClick: () -> Unit = {},
    screenType: ScreenType,
    onEmptyTrash: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val keyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()
        }
    )

    when (screenType) {
        ScreenType.Archive -> {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
//                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                title = { Text(title, Modifier.padding(start = 16.dp)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSearchClick()
                        focusRequester.requestFocus()
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }

        ScreenType.List -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSearchClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        IconButton(onMenuClick) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Menu"
                            )
                        }

                        Spacer(Modifier.width(40.dp))

                        Text("Search notes..")
                    }

                    IconButton(onThemeClick) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = "Change Theme"
                        )
                    }

                    IconButton(onGridLayoutClick) {
                        Icon(
                            Icons.Outlined.TableRows,
                            contentDescription = "Change Grid Layout"
                        )
                    }

                    Spacer(Modifier.width(8.dp))


                }
            }
        }

        ScreenType.Trash -> {
            var showMoreMenu by remember { mutableStateOf(false) }

            TopAppBar(
//                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                title = { Text("Trash", Modifier.padding(start = 16.dp)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Empty Trash") },
                            onClick = {
                                showMoreMenu = false
                                onEmptyTrash()
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    }

}






















