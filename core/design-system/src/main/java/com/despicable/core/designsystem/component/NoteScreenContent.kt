package com.despicable.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.despicable.core.designsystem.R
import com.despicable.core.model.NoteWithTags

/*@Composable
fun NoteScreenContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    notes: List<NoteWithTags>,
    isInitialized: Boolean,
    searchQuery: String,
    emptyIcon: Int,
    emptyTitle: String,
    content: @Composable () -> Unit
) {
    Crossfade(
        modifier = modifier.padding(paddingValues = paddingValues),
        targetState = Triple(
            notes.isEmpty(),
            isInitialized,
            searchQuery.isEmpty()
        ),
        label = ""
    ) { (isEmpty, initialized, noSearch) ->
        when {
            isEmpty && initialized && noSearch -> {
                EmptyStateContent(
                    icon = emptyIcon,
                    title = emptyTitle
                )
            }

            isEmpty && searchQuery.isNotEmpty() -> {
                EmptyStateContent(
                    icon = R.drawable.search,
                    title = "No notes match your search"
                )
            }

            else -> content()
        }
    }
}*/

@Composable
fun NoteScreenContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    notes: List<NoteWithTags>,
    isInitialized: Boolean,
    searchQuery: String,
    emptyIcon: Int,
    emptyTitle: String,
    content: @Composable () -> Unit
) {
    Crossfade(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        targetState = getNoteScreenState(
            notes = notes,
            isInitialized = isInitialized,
            searchQuery = searchQuery,
        ),
        label = ""
    ) { state ->
        when (state) {
            NoteScreenState.EmptyInitial ->
                EmptyStateContent(
                    icon = emptyIcon,
                    title = emptyTitle
                )

            NoteScreenState.EmptySearch ->
                EmptyStateContent(
                    icon = R.drawable.search,
                    title = "No notes match your search"
                )

            NoteScreenState.Content -> content()
        }
    }
}


// Enum to represent different screen states
enum class NoteScreenState {
    EmptyInitial,
    EmptySearch,
    Content
}

// Helper function to determine the screen state
fun getNoteScreenState(
    notes: List<NoteWithTags>,
    isInitialized: Boolean,
    searchQuery: String,
): NoteScreenState {

    // Existing empty state checks
    return when {
        notes.isEmpty() && isInitialized && searchQuery.isEmpty() ->
            NoteScreenState.EmptyInitial

        notes.isEmpty() && searchQuery.isNotEmpty() ->
            NoteScreenState.EmptySearch

        else -> NoteScreenState.Content
    }
}