package com.despicable.feature.detail.checklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.androidx.compose.koinViewModel
import javax.inject.Inject

// ChecklistItem.kt
data class ChecklistItemData(
    val id: Long,
    val content: String,
    val isChecked: Boolean,
    val position: Int
)

// KeepStyleChecklist.kt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepStyleChecklist2(
    modifier: Modifier = Modifier,
    items: List<ChecklistItemData>,
    onItemChange: (ChecklistItemData) -> Unit,
    onItemDelete: (ChecklistItemData) -> Unit,
    onItemMove: (Int, Int) -> Unit,
    onAddNewItem: (String) -> Unit
) {
    var draggedItem by remember { mutableStateOf<ChecklistItemData?>(null) }
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            val currentItem by rememberUpdatedState(item)

            LazyItemDraggableContainer(
                modifier = Modifier.animateItemPlacement(),
                draggedItem = draggedItem,
                itemKey = item.id,
                onDragStart = { draggedItem = currentItem },
                onDragEnd = { finalPosition ->
                    draggedItem?.let { draggedItem ->
                        val fromIndex = items.indexOfFirst { it.id == draggedItem.id }
                        val toIndex = finalPosition.coerceIn(0, items.size - 1)
                        if (fromIndex != -1 && fromIndex != toIndex) {
                            onItemMove(fromIndex, toIndex)
                        }
                    }
                    draggedItem = null
                }
            ) {
                ChecklistItemRow(
                    item = item,
                    onItemChange = onItemChange,
                    onItemDelete = onItemDelete,
                    onNewItemRequest = { cursorPosition ->
                        val newItem = ChecklistItemData(
                            id = System.currentTimeMillis(),
                            content = "",
                            isChecked = false,
                            position = item.position + 1
                        )
                        onItemChange(newItem)
                    }
                )
            }
        }

        item {
            AddNewItemRow2(onNewItem = onAddNewItem)
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItemData,
    onItemChange: (ChecklistItemData) -> Unit,
    onItemDelete: (ChecklistItemData) -> Unit,
    onNewItemRequest: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(item.id) {
        mutableStateOf(TextFieldValue(item.content, TextRange(item.content.length)))
    }
    var focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = "Drag to reorder",
            modifier = Modifier.padding(end = 8.dp)
        )

        // Checkbox
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { isChecked ->
                onItemChange(item.copy(isChecked = isChecked))
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Content TextField
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onItemChange(item.copy(content = newValue.text))
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    onNewItemRequest(textFieldValue.selection.start)
                }
            )
        )

        // Delete button
        IconButton(onClick = { onItemDelete(item) }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete item",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    LaunchedEffect(item.id) {
        if (item.content.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun AddNewItemRow2(
    onNewItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { focusRequester.requestFocus() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp, end = 12.dp)
        )

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge,
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "List item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onNewItem(text)
                        text = ""
                    }
                }
            )
        )
    }
}

// Draggable container implementation
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemDraggableContainer(
    modifier: Modifier = Modifier,
    draggedItem: ChecklistItemData?,
    itemKey: Long,
    onDragStart: () -> Unit,
    onDragEnd: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val isDragging = draggedItem?.id == itemKey
    val draggableState = rememberDraggableState { }

    Box(
        modifier = modifier
            .then(
                if (isDragging) {
                    Modifier.shadow(8.dp)
                } else {
                    Modifier
                }
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = { onDragStart() },
                onDragStopped = { velocity ->
                    onDragEnd((velocity / 100).toInt())
                }
            )
    ) {
        content()
    }
}

// PreviewChecklistScreen.kt
@Composable
fun PreviewChecklistScreen2(
    viewModel: PreviewChecklistViewModel2 = koinViewModel()
) {
    val items by viewModel.items.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Shopping List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Checklist
        KeepStyleChecklist2(
            items = items,
            onItemChange = viewModel::updateItem,
            onItemDelete = viewModel::deleteItem,
            onItemMove = viewModel::moveItem,
            onAddNewItem = viewModel::addItem,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// PreviewChecklistViewModel.kt
class PreviewChecklistViewModel2 : ViewModel() {
    private val _items = MutableStateFlow(SampleData2.initialChecklistItems)
    val items: StateFlow<List<ChecklistItemData>> = _items.asStateFlow()

    fun addItem(content: String) {
        if (content.isBlank()) return

        val newItem = ChecklistItemData(
            id = System.currentTimeMillis(),
            content = content,
            isChecked = false,
            position = _items.value.size
        )
        _items.update { currentItems -> currentItems + newItem }
    }

    fun updateItem(item: ChecklistItemData) {
        _items.update { currentItems ->
            currentItems.map { if (it.id == item.id) item else it }
        }
    }

    fun deleteItem(item: ChecklistItemData) {
        _items.update { currentItems ->
            currentItems.filter { it.id != item.id }
                .mapIndexed { index, checklistItem ->
                    checklistItem.copy(position = index)
                }
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        _items.update { currentItems ->
            val mutableItems = currentItems.toMutableList()
            val item = mutableItems.removeAt(fromPosition)
            mutableItems.add(toPosition, item)
            mutableItems.mapIndexed { index, checklistItem ->
                checklistItem.copy(position = index)
            }
        }
    }
}

object SampleData2 {
    val initialChecklistItems = listOf(
        ChecklistItemData(
            id = 1L,
            content = "Milk",
            isChecked = false,
            position = 0
        ),
        ChecklistItemData(
            id = 2L,
            content = "Bread",
            isChecked = true,
            position = 1
        ),
        ChecklistItemData(
            id = 3L,
            content = "Eggs",
            isChecked = false,
            position = 2
        )
    )
}



// ViewModel
class ChecklistViewModel @Inject constructor() : ViewModel() {
    private val _items = MutableStateFlow<List<ChecklistItemData>>(emptyList())
    val items: StateFlow<List<ChecklistItemData>> = _items.asStateFlow()

    fun addItem(content: String) {
        val newItem = ChecklistItemData(
            id = System.currentTimeMillis(),
            content = content,
            isChecked = false,
            position = _items.value.size
        )
        _items.update { currentItems -> currentItems + newItem }
    }

    fun updateItem(item: ChecklistItemData) {
        _items.update { currentItems ->
            currentItems.map { if (it.id == item.id) item else it }
        }
    }

    fun deleteItem(item: ChecklistItemData) {
        _items.update { currentItems ->
            currentItems.filter { it.id != item.id }
                .mapIndexed { index, checklistItem ->
                    checklistItem.copy(position = index)
                }
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        _items.update { currentItems ->
            val mutableItems = currentItems.toMutableList()
            val item = mutableItems.removeAt(fromPosition)
            mutableItems.add(toPosition, item)
            mutableItems.mapIndexed { index, checklistItem ->
                checklistItem.copy(position = index)
            }
        }
    }
}

/*
class ChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = checkStateHandle.get<Long>("noteId") ?: 0L

    val checklistItems: StateFlow<List<ChecklistEntity>> =
        checklistRepository.getChecklistItemsFlow(noteId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addItem(content: String) {
        viewModelScope.launch {
            checklistRepository.addChecklistItem(noteId, content)
        }
    }

    fun toggleItem(item: ChecklistEntity) {
        viewModelScope.launch {
            checklistRepository.toggleChecklistItem(item)
        }
    }

    fun deleteItem(item: ChecklistEntity) {
        viewModelScope.launch {
            checklistRepository.deleteChecklistItem(item)
        }
    }

    fun reorderItems(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            checklistRepository.reorderChecklistItems(noteId, fromPosition, toPosition)
        }
    }
}*/
