package com.despicable.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.despicable.core.designsystem.component.EmptyStateContent
import com.despicable.core.model.NoteComplete

@Composable
fun NoteScreenContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    notes: List<NoteComplete>,
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
                    title = stringResource(R.string.home_no_notes_match_your_search)
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
    notes: List<NoteComplete>,
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