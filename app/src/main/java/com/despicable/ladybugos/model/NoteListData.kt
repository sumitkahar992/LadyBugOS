package com.despicable.ladybugos.model

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp


@Composable
fun NoteListScreen() {
    val scrollState = rememberLazyListState()
    var showSearchBar by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Detect scroll direction
                val delta = available.y
                if (delta > 0) { // Scrolling down
                    showSearchBar = true
                } else if (delta < 0) { // Scrolling up
                    // Only hide if we can scroll up (not at the top)
                    val canScrollUp = scrollState.firstVisibleItemIndex > 0 ||
                            scrollState.firstVisibleItemScrollOffset > 0
                    if (canScrollUp) {
                        showSearchBar = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Reset window insets
    ) { innerPadding ->

        Box(Modifier.fillMaxSize().padding(innerPadding)) {


            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = if (showSearchBar) 56.dp else 0.dp)
            ) {
                items(100) { note ->
                    NoteItem(
                        note = NoteItem(
                            id = note,
                            title = "Title $note",
                            content = "Content $note"
                        )
                    )
                }
            }

            // Status bar background (MUST COME AFTER CONTENT TO LAYER CORRECTLY)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.systemBars)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            )



            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchBar(
                    onSearch = { query ->
                        // Handle search here
                    },
                    onFocusChange = { isFocused ->
                    }
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    onSearch: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search notes") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        LaunchedEffect(isSearchFocused) {
            onFocusChange(isSearchFocused)
        }
    }
}

@Composable
fun NoteItem(note: NoteItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Data class for notes
data class NoteItem(val id: Int, val title: String, val content: String)