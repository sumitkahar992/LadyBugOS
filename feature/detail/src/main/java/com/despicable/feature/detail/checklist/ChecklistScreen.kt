package com.despicable.feature.detail.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.compose.koinViewModel

// SampleData.kt
object SampleData {
    val sampleNote = NoteEntity(
        id = 1L,
        title = "Shopping List",
        content = "Things to buy",
        updateDate = "2025-01-01",
        lightColor = 0xFFE0E0E0.toInt(),
        isChecklist = true
    )

    val sampleChecklistItems = listOf(
        ChecklistEntity(
            id = 1L,
            noteId = 1L,
            content = "Milk",
            isChecked = false,
            position = 0
        ),
        ChecklistEntity(
            id = 2L,
            noteId = 1L,
            content = "Bread",
            isChecked = true,
            position = 1
        ),
        ChecklistEntity(
            id = 3L,
            noteId = 1L,
            content = "Eggs",
            isChecked = false,
            position = 2
        )
    )
}

// PreviewChecklistViewModel.kt
class PreviewChecklistViewModel : ViewModel() {
    private val _checklistItems = MutableStateFlow(SampleData.sampleChecklistItems)
    val checklistItems: StateFlow<List<ChecklistEntity>> = _checklistItems.asStateFlow()

    fun addItem(content: String) {
        val currentItems = _checklistItems.value.toMutableList()
        val newItem = ChecklistEntity(
            id = (currentItems.maxOfOrNull { it.id } ?: 0) + 1,
            noteId = 1L,
            content = content,
            position = currentItems.size
        )
        currentItems.add(newItem)
        _checklistItems.value = currentItems
    }

    fun toggleItem(item: ChecklistEntity) {
        val currentItems = _checklistItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentItems[index] = item.copy(isChecked = !item.isChecked)
            _checklistItems.value = currentItems
        }
    }

    fun deleteItem(item: ChecklistEntity) {
        val currentItems = _checklistItems.value.toMutableList()
        currentItems.removeAll { it.id == item.id }
        // Update positions
        currentItems.forEachIndexed { index, checklistItem ->
            currentItems[index] = checklistItem.copy(position = index)
        }
        _checklistItems.value = currentItems
    }

    fun reorderItems(fromPosition: Int, toPosition: Int) {
        val currentItems = _checklistItems.value.toMutableList()
        val item = currentItems.removeAt(fromPosition)
        currentItems.add(toPosition, item)
        // Update positions
        currentItems.forEachIndexed { index, checklistItem ->
            currentItems[index] = checklistItem.copy(position = index)
        }
        _checklistItems.value = currentItems
    }
}


// ChecklistPreview.kt
@Preview(showBackground = true)
@Composable
fun ChecklistScreenPreview() {
    val previewViewModel = remember { PreviewChecklistViewModel() }
    LadyBugOSTheme {
        ChecklistScreen(viewModel = previewViewModel)

    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistItemPreview() {
    LadyBugOSTheme {
        ChecklistItem(
            item = SampleData.sampleChecklistItems[0],
            onToggle = {},
            onDelete = {}
        )
    }
}

// Updated ChecklistScreen.kt
@Composable
fun ChecklistScreen(
    viewModel: PreviewChecklistViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val checklistItems by viewModel.checklistItems.collectAsState()
    var newItemText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = SampleData.sampleNote.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Add new item section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text("Add item") },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    if (newItemText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.addItem(newItemText)
                                newItemText = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add item")
                        }
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = checklistItems,
                key = { it.id }
            ) { item ->
                ChecklistItem(
                    item = item,
                    onToggle = { viewModel.toggleItem(item) },
                    onDelete = { viewModel.deleteItem(item) }
                )
            }
        }
    }
}

// Updated ChecklistItem.kt
@Composable
fun ChecklistItem(
    item: ChecklistEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() }
            )

            Text(
                text = item.content,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}