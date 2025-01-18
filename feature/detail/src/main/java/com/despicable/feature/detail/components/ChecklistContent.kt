package com.despicable.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.despicable.feature.detail.ChecklistItemUiState


fun LazyListScope.checklistContent(
    items: List<ChecklistItemUiState>,
    onItemChecked: (Int) -> Unit,
    onItemContentChange: (Int, TextFieldValue) -> Unit,
    onItemRemove: (Int) -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    focusManager: FocusManager
) {

    items(
        items = items,
        key = { it.id }
    ) { item ->
        val itemFocusRequester = remember { FocusRequester() }

        ChecklistItem(
            item = item,
            onCheckedChange = { onItemChecked(item.position) },
            onContentChange = { onItemContentChange(item.position, it) },
            onRemove = { onItemRemove(item.position) },
            onNext = {
                if (item.position == items.lastIndex) {
                    onAddItem()
                    focusManager.moveFocus(FocusDirection.Down)
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            },
            focusRequester = itemFocusRequester,
            modifier = Modifier.fillMaxWidth()
        )
    }

    item {
        AddItem(
            onClick = onAddItem,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChecklistItem(
    item: ChecklistItemUiState,
    focusRequester: FocusRequester,
    onCheckedChange: () -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDelete by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showDelete = true }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.padding(8.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        )

        BasicTextField(
            value = item.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.isChecked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isChecked)
                    TextDecoration.LineThrough
                else
                    TextDecoration.None
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )

        AnimatedVisibility(
            visible = showDelete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AddItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add item",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = "List item",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
/*fun LazyListScope.checklistContent(
    items: List<ChecklistItemUiState>,
    onItemChecked: (Int) -> Unit,
    onItemContentChange: (Int, TextFieldValue) -> Unit,
    onItemRemove: (Int) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
    // Keep track of focus state


    items(
        items = items,
        key = { it.id }
    ) { item ->
        val itemFocusRequester = remember { FocusRequester() }

        ChecklistItem(
            item = item,
            onCheckedChange = { onItemChecked(item.position) },
            onContentChange = { onItemContentChange(item.position, it) },
            onRemove = { onItemRemove(item.position) },
            onNext = {
                // Move focus to next item or add button
                if (item.position < items.lastIndex) {
                    focusManager.moveFocus(FocusDirection.Down)
                } else {
                    focusRequester.requestFocus()
                }
            },
            focusRequester = itemFocusRequester,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }

    item(key = "add_button") {
        AddChecklistItemButton(
            onClick = {
                onAddItem()
                // Focus the newly added item
                focusManager.moveFocus(FocusDirection.Up)
            },
            focusRequester = focusRequester,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ChecklistItem(
    item: ChecklistItemUiState,
    onCheckedChange: () -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled
        )

        BasicTextField(
            value = item.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .focusRequester(focusRequester),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() }
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )

        IconButton(
            onClick = onRemove,
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove item"
            )
        }
    }
}

@Composable
fun AddChecklistItemButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .focusRequester(focusRequester),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add checklist item",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Add item",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}*/
/*
fun LazyListScope.checklistContent(
    items: List<ChecklistItemUiState>,
    onItemChecked: (Int) -> Unit,
    onItemContentChange: (Int, TextFieldValue) -> Unit,
    onItemRemove: (Int) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    items(
        items = items,
        key = { it.id }  // Use the unique ID as the key
    ) { item ->
        ChecklistItem(
            item = item,
            onCheckedChange = { onItemChecked(item.position) },
            onContentChange = { onItemContentChange(item.position, it) },
            onRemove = { onItemRemove(item.position) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }

    item(key = "add_button") {  // Add a unique key for the add button
        AddChecklistItemButton(
            onClick = onAddItem,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ChecklistItem(
    item: ChecklistItemUiState,
    onCheckedChange: () -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled
        )

        BasicTextField(
            value = item.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )

        IconButton(
            onClick = onRemove,
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AddChecklistItemButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add checklist item",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Add item",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
*/

