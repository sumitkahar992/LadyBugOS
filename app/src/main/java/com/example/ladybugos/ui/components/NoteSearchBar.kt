package com.example.ladybugos.ui.components


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ladybugos.model.Note
import kotlinx.coroutines.delay

/*
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ExpandableSearchViewPreview() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(Theme.Light) }
    var selectedNotes by remember { mutableStateOf(setOf<Note>()) }

    MaterialTheme {
        Column {
            // Collapsed state
            PreviewSection("Collapsed State") {
                ExpandableSearchView(
                    onMenuClick = {},
                    selectedTheme = selectedTheme,
                    onThemeChanged = { selectedTheme = it },
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onSearchClosed = {},
                    selectedNotes = emptySet(),
                    onClearSelection = {},
                    onPinNotes = {},
                    onArchiveNotes = {},
                    onDeleteNotes = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Expanded search state
            PreviewSection("Expanded Search State") {
                ExpandableSearchView(
                    onMenuClick = {},
                    selectedTheme = selectedTheme,
                    onThemeChanged = { selectedTheme = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onSearchClosed = {},
                    selectedNotes = emptySet(),
                    onClearSelection = {},
                    onPinNotes = {},
                    onArchiveNotes = {},
                    onDeleteNotes = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Selection state
            PreviewSection("Selection State") {
                ExpandableSearchView(
                    onMenuClick = {},
                    selectedTheme = selectedTheme,
                    onThemeChanged = { selectedTheme = it },
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onSearchClosed = {},
                    selectedNotes = setOf(
                        Note(1, "Note 1", "Content 1", System.currentTimeMillis().toString()),
                        Note(2, "Note 2", "Content 2", System.currentTimeMillis().toString())
                    ),
                    onClearSelection = {},
                    onPinNotes = {},
                    onArchiveNotes = {},
                    onDeleteNotes = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PreviewSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
*/


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableSearchView(
    onMenuClick: () -> Unit,
    searchQuery: String,
    onGridLayoutClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClosed: () -> Unit,
    selectedNotes: Set<Note>,
    onClearSelection: () -> Unit,
    onPinNotes: (List<Note>) -> Unit,
    onUnPinNotes: (List<Note>) -> Unit,
    onArchiveNotes: (List<Note>) -> Unit,
    onDeleteNotes: (List<Note>) -> Unit,
    modifier: Modifier = Modifier,
    isSearchBarVisible: MutableState<Boolean>,
    onSetReminder: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(100) // Add a small delay before focusing
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    AnimatedVisibility(
        visible = isSearchBarVisible.value,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                val transitionState = remember { MutableTransitionState(false) }
                transitionState.targetState = selectedNotes.isNotEmpty() || isExpanded

                val transition = rememberTransition(transitionState, label = "searchTransition")

                val expandProgress by transition.animateFloat(
                    transitionSpec = {
                        if (targetState) {
                            tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        } else {
                            tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        }
                    },
                    label = "expandProgress"
                ) { state -> if (state) 1f else 0f }

                val collapsedAlpha by transition.animateFloat(
                    transitionSpec = { tween(durationMillis = 200) },
                    label = "collapsedAlpha"
                ) { state -> if (state) 0f else 1f }

                // CollapsedSearchView (always present, fades out when not active)
                CollapsedSearchView(
                    onExpandedChanged = { isExpanded = true },
                    modifier = Modifier.alpha(collapsedAlpha),
                    onMenuClick = onMenuClick,
                    onGridLayoutClick = onGridLayoutClick
                )

                // Expanded search view and Selection top bar container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = expandProgress
                            scaleY = 0.8f + (0.2f * expandProgress)
                            translationY = (-20f + (20f * expandProgress)).dp.toPx()
                        }
                ) {
                    when {
                        selectedNotes.isNotEmpty() -> {
                            SelectionTopBar(
                                onClearSelection = onClearSelection,
                                onPinNotes = { onPinNotes(selectedNotes.toList()) },
                                onArchiveNotes = { onArchiveNotes(selectedNotes.toList()) },
                                onDeleteNotes = { onDeleteNotes(selectedNotes.toList()) },
                                selectedNotes = selectedNotes,
                                onUnpinNotes = { onUnPinNotes(selectedNotes.toList()) },
                                onUnarchiveNotes = {},
                                screenType = ScreenType.List,
                                onSetReminder = onSetReminder
                            )
                        }

                        isExpanded -> {
                            ExpandedSearchView(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = onSearchQueryChanged,
                                onBackClick = {
                                    isExpanded = false
                                    onSearchQueryChanged("")
                                },
                                onSearchClosed = onSearchClosed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                    }
                }
            }
        }

    }

}


sealed class ScreenType {
    object List : ScreenType()
    object Archive : ScreenType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    onClearSelection: () -> Unit,
    onPinNotes: () -> Unit = {},
    onUnpinNotes: () -> Unit = {},
    onSetReminder: () -> Unit,
    onArchiveNotes: () -> Unit = {},
    onUnarchiveNotes: () -> Unit = {},
    onDeleteNotes: () -> Unit,
    selectedNotes: Set<Note>,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    screenType: ScreenType
) {
    TopAppBar(
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
                        Icon(Icons.Outlined.NotificationAdd, contentDescription = "Reminder notes")
                    }
                    IconButton(onClick = onArchiveNotes) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Archive notes")
                    }

                    IconButton(onClick = onDeleteNotes) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete notes")
                    }
                }

                ScreenType.Archive -> {
                    IconButton(onClick = onPinNotes) {
                        Icon(Icons.Outlined.PushPin, contentDescription = "Pin notes")
                    }
                    IconButton(onClick = onUnarchiveNotes) {
                        Icon(Icons.Outlined.Unarchive, contentDescription = "Unarchive notes")
                    }
                    IconButton(onClick = onDeleteNotes) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete notes")
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
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


/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onPinNotes: () -> Unit,
    onArchiveNotes: () -> Unit,
    onDeleteNotes: () -> Unit,
    selectedNotes: Set<Note>, // New parameter to access the selected notes
    modifier: Modifier = Modifier
) {
    val isAllPinned = selectedNotes.all { it.isPinned }
    val isAnyUnpinned = selectedNotes.any { !it.isPinned }

    val isAllArchived = selectedNotes.all { it.isArchived }
    val isAnyUnarchived = selectedNotes.any { !it.isArchived }

    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onPinNotes) {
                Icon(
                    imageVector = when {
                        isAllPinned -> Icons.Filled.PushPin // Show filled icon if all selected are pinned
                        isAnyUnpinned -> Icons.Outlined.PushPin // Show outlined icon if any unpinned notes are selected
                        else -> Icons.Outlined.PushPin // Default to outlined if nothing is selected
                    },
                    contentDescription = "Pin notes",
                    tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onArchiveNotes) {
                Icon(
                    imageVector = when {
                        isAllArchived -> Icons.Filled.Archive
                        isAnyUnarchived -> Icons.Outlined.Archive
                        else -> Icons.Outlined.Archive
                    },
                    contentDescription = "Archive notes",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDeleteNotes) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notes",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = modifier
    )
}
*/

@Composable
fun ExpandedSearchView(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClosed: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text("Search your notes", color = Color.Gray) },
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 44.dp),
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                }
            }
        },
        leadingIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchClosed() }),
        singleLine = true
    )
}


@Composable
fun CollapsedSearchView(
    onMenuClick: () -> Unit,
    onGridLayoutClick: () -> Unit,
    onExpandedChanged: () -> Unit,
    modifier: Modifier = Modifier
) {


    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 44.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandedChanged),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    modifier = Modifier.clickable { onMenuClick() },
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Menu"
                )
                Spacer(Modifier.width(40.dp))
//                Icon(Icons.Default.Search, contentDescription = "Search")
//                Spacer(Modifier.width(12.dp))
                Text("Search notes...", color = Color.Gray)
            }
            IconButton(
                onClick = onGridLayoutClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable(onClick = { })
            ) {
                Icon(Icons.Outlined.TableRows, contentDescription = "Change Grid Layout")
            }
        }
    }
}






























