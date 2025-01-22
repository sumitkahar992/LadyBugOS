package com.despicable.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.despicable.core.model.Checklist


@Composable
fun ChecklistItem(
    dragModifier: Modifier = Modifier,
    item: Checklist,
    onCheckedChange: (Boolean) -> Unit,
    onContentChange: (String) -> Unit,
    onDelete: () -> Unit,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    shouldFocus: Boolean,
    onFocusChange: (Boolean) -> Unit,
    isDragging: Boolean
) {
    var textFieldValue by remember(item.id, item.content) {
        mutableStateOf(
            TextFieldValue(
                text = item.content,
                selection = TextRange(item.content.length)
            )
        )
    }

    var isTextFieldFocused by remember { mutableStateOf(false) }


    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "elevation",
    )


    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            focusRequester.requestFocus()
        }
    }


    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = if (isDragging) {
            BorderStroke(0.1.dp, MaterialTheme.colorScheme.onSurface)
        } else null
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DragHandle(dragModifier)

            ChecklistCheckbox(
                isChecked = item.isChecked,
                onCheckedChange = onCheckedChange
            )

            ChecklistTextField(
                value = textFieldValue,
                isChecked = item.isChecked,
                focusRequester = focusRequester,
                onFocusChange = { isFocused ->
                    onFocusChange(isFocused)
                    isTextFieldFocused = isFocused
                },
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onContentChange(newValue.text)
                },
                onNext = onNext
            )


            AnimatedVisibility(
                visible = isTextFieldFocused,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                DeleteButton(onClick = onDelete)
            }
        }
    }
}


@Composable
fun AddItemButton(
    onAddClick: () -> Unit
) {
    TextButton(
        onClick = onAddClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 33.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add list item",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "List item",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
private fun DragHandle(dragModifier: Modifier) {
    IconButton(
        modifier = dragModifier.size(30.dp),
        onClick = {}
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = "Reorder",
        )
    }
}

@Composable
private fun ChecklistCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Checkbox(
        checked = isChecked,
        onCheckedChange = { onCheckedChange(it) },
        modifier = Modifier.padding(end = 4.dp),
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}


@Composable
private fun RowScope.ChecklistTextField(
    value: TextFieldValue,
    isChecked: Boolean,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onNext: () -> Unit
) {
    Box(modifier = Modifier.weight(1f)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->

                innerTextField()
                /*    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty()) {
                            Text(
                                text = "List item",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }*/
            }
        )
    }
}


@Composable
private fun DeleteButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Delete item",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

}

