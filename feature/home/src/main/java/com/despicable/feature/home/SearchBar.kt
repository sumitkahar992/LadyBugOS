package com.despicable.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.component.ScreenType
import com.despicable.core.designsystem.component.SelectionTopBar
import com.despicable.model.Note
import timber.log.Timber

@Composable
fun SearchBarWithActions(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onLayoutClick: () -> Unit,
    isSearchMode: Boolean = false,
    onBackClick: () -> Unit = {},
    isVisible: Boolean,
    selectedNotes: Set<Note>,
    onClearSelection: () -> Unit,
    onPinNotes: (List<Note>) -> Unit,
    onUnPinNotes: (List<Note>) -> Unit,
    onArchiveNotes: (List<Note>) -> Unit,
    onDeleteNotes: (List<Note>) -> Unit,
    onSetReminder: () -> Unit,
    modifier: Modifier = Modifier,
    screenType: ScreenType = ScreenType.List
) {
    val isFocused = rememberSaveable { mutableStateOf(isSearchMode) }
    val startPaddings by animateDpAsState(
        if (isFocused.value || selectedNotes.isNotEmpty()) 0.dp else startWindowInsetsPadding() + 12.dp,
        label = "horizontalPaddings"
    )
    val endPaddings by animateDpAsState(
        if (isFocused.value || selectedNotes.isNotEmpty()) 0.dp else endWindowInsetsPadding() + 12.dp,
        label = "horizontalPaddings"
    )

    val topPadding by animateDpAsState(
        if (isFocused.value || selectedNotes.isNotEmpty()) 0.dp else topWindowInsetsPadding() + 10.dp,
        label = "topPadding"
    )

    val height by animateDpAsState(
        if (isFocused.value || selectedNotes.isNotEmpty()) topWindowInsetsPadding() + SearchBarHeight.dp else SearchBarHeight.dp - 10.dp,
        label = "height"
    )

    val cornerRadius by animateDpAsState(
        if (isFocused.value || selectedNotes.isNotEmpty()) 0.dp else 15.dp,
        label = "cornerRadius"
    )

    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = isVisible || isFocused.value,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = selectedNotes.isNotEmpty(),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(
                            animationSpec = tween(
                                durationMillis = 150,
                                delayMillis = 90,
                                easing = FastOutSlowInEasing
                            )
                        ) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = 90,
                                easing = FastOutSlowInEasing
                            )
                        )).togetherWith(
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 100,
                                    delayMillis = 50,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleOut(
                                targetScale = 1.02f,
                                animationSpec = tween(
                                    durationMillis = 200,
                                    delayMillis = 50,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        )
                    } else {
                        (fadeIn(
                            animationSpec = tween(
                                durationMillis = 150,
                                delayMillis = 90,
                                easing = FastOutSlowInEasing
                            )
                        ) + scaleIn(
                            initialScale = 1.02f,
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = 90,
                                easing = FastOutSlowInEasing
                            )
                        )).togetherWith(
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 100,
                                    delayMillis = 50,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + scaleOut(
                                targetScale = 0.92f,
                                animationSpec = tween(
                                    durationMillis = 200,
                                    delayMillis = 50,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "SearchBarSelection"
            ) { isSelection ->
                if (isSelection) {
                    SelectionTopBar(
                        cornerRadius = cornerRadius,
                        onClearSelection = onClearSelection,
                        onPinNotes = { onPinNotes(selectedNotes.toList()) },
                        onUnpinNotes = { onUnPinNotes(selectedNotes.toList()) },
                        onSetReminder = onSetReminder,
                        onArchiveNotes = { onArchiveNotes(selectedNotes.toList()) },
                        onDeleteNotes = { onDeleteNotes(selectedNotes.toList()) },
                        selectedNotes = selectedNotes,
                        screenType = screenType
                    )

                } else {
                    // Show regular search bar when no selection
                    SearchBar(
                        modifier = modifier
                            .padding(
                                top = topPadding,
                                start = startPaddings,
                                end = endPaddings
                            )
                            .heightIn(max = height),
                        isFocused = isFocused,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onMenuClick = onMenuClick,
                        onLayoutClick = onLayoutClick,
                        onBackClick = {
                            onSearchQueryChange("")
                            focusManager.clearFocus()
                            isFocused.value = false
                            onBackClick()
                        }
                    )
                }
            }
        }
    }

    // Update BackHandler to handle both search and selection
    BackHandler(enabled = isFocused.value || searchQuery.isNotEmpty() || selectedNotes.isNotEmpty()) {
        when {
            selectedNotes.isNotEmpty() -> onClearSelection()
            else -> {
                onSearchQueryChange("")
                focusManager.clearFocus()
                isFocused.value = false
                onBackClick()
            }
        }
    }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            isFocused.value = true
        }
    }

    Timber.tag("DEBUG").d("[iS FOCUSED]- [${isFocused.value}]")

}

@Composable
private fun MenuButton(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    isFocused: MutableState<Boolean>
) {

    if (isFocused.value) {
        Button(
            modifier = Modifier.fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            onClick = {
                isFocused.value = false
                onBackClick()
            },
            elevation = null,
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Button(
            modifier = Modifier
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            onClick = onMenuClick,
            elevation = null,
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Calendar",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

    }


}

@Composable
private fun LayoutButton(
    searchQuery: String,
    onCloseClick: () -> Unit,
    onLayoutClick: () -> Unit,
    isFocused: MutableState<Boolean>
) {
    if (isFocused.value) {
        AnimatedVisibility(
            modifier = Modifier.padding(end = 16.dp),
            visible = searchQuery.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            IconButton(
                modifier = Modifier.size(30.dp),
                onClick = onCloseClick,
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    imageVector = Icons.Default.Clear,
                    contentDescription = null
                )
            }
        }
    } else {
        AnimatedVisibility(
            modifier = Modifier.heightIn(max = SearchBarHeight.dp),
            visible = !isFocused.value,
            enter = slideInHorizontally { it / 2 } + expandHorizontally(
                expandFrom = Alignment.Start,
                clip = false
            ) + fadeIn(),
            exit = slideOutHorizontally { -it / 2 } + shrinkHorizontally(
                shrinkTowards = Alignment.Start,
                clip = false
            ) + fadeOut(),
        ) {
            // Color(0xFFE3E3E3)
            Button(
                modifier = Modifier
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                onClick = onLayoutClick,
                elevation = null,
//            contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.TableRows,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }


}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    isFocused: MutableState<Boolean>,
    onMenuClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val topPadding by animateDpAsState(
        if (isFocused.value) topWindowInsetsPadding() else 0.dp,
        label = "topPadding"
    )
    val cornerRadius by animateDpAsState(
        if (isFocused.value) 0.dp else 15.dp,
        label = "cornerRadius"
    )
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val keyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()
        }
    )
    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusRequester.requestFocus() }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(cornerRadius)
            )
            .fillMaxHeight()
//            .padding(horizontal = 15.dp)
            .padding(top = topPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuButton(
            onMenuClick = onMenuClick,
            isFocused = isFocused,
            onBackClick = onBackClick
        )

        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused.value = it.isFocused },
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search your notes",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSecondaryContainer),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            )
        )

        LayoutButton(
            onLayoutClick = onLayoutClick,
            isFocused = isFocused,
            searchQuery = searchQuery,
            onCloseClick = {
                onSearchQueryChange("")
            }
        )
    }


}

/*

@Keep
internal enum class Keyboard {
    Opened, Closed
}

@Composable
internal fun keyboardAsState(): State<Keyboard> {
    val keyboardState = remember { mutableStateOf(Keyboard.Closed) }
    val view = LocalView.current
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
                Keyboard.Opened
            } else {
                Keyboard.Closed
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}*/
/*   LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible == Keyboard.Closed) {
            isFocused.value = false
            focusManager.clearFocus()
        }
    }*/