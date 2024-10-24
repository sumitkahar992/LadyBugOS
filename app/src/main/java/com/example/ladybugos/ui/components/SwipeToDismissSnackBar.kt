package com.example.ladybugos.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun SwipeToDismissContentSnack(
    onSwipeToDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    coroutineScope.launch {
                        onSwipeToDismiss()
                    }
                }

                SwipeToDismissBoxValue.Settled -> {}
            }
            false
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {},
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        content = { content() },
    )
}