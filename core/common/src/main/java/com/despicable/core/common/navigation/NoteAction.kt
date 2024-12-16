package com.despicable.core.common.navigation

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController

sealed interface NoteAction {
    val noteId: Long
    val message: String

    data class Delete(
        override val noteId: Long,
        override val message: String = "Note moved to trash"
    ) : NoteAction

    data class Archive(
        override val noteId: Long,
        override val message: String = "Note archived"
    ) : NoteAction

    data class Unarchive(
        override val noteId: Long,
        override val message: String = "Note unarchived"
    ) : NoteAction
}

/*
    Types of actions that can be performed on a note
*/
enum class NoteActionType {
    DELETE,
    ARCHIVE,
    UNARCHIVE;

    companion object {
        fun fromString(value: String?): NoteActionType? = try {
            value?.let { valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

fun NavController.handleAction(action: NoteAction) {
    previousBackStackEntry?.savedStateHandle?.apply {
        set("noteId", action.noteId)
        set(
            "actionType", when (action) {
                is NoteAction.Delete -> NoteActionType.DELETE
                is NoteAction.Archive -> NoteActionType.ARCHIVE
                is NoteAction.Unarchive -> NoteActionType.UNARCHIVE
            }.name
        )
    }
    popBackStack()
}

/*
   Manages the state of note actions (delete, archive, unarchive)
*/
@Stable
class ActionState(
    val noteId: Long?,
    val actionType: NoteActionType?,
) {
    fun clear(backStackEntry: NavBackStackEntry?) {
        /*
            Extension function to clear note action from saved state
        */
        backStackEntry?.savedStateHandle?.apply {
            remove<Long>("noteId")
            remove<String>("actionType")
        }
    }
}


/*
   Remembers the current note action state
*/
@Composable
 fun rememberActionState(backStackEntry: NavBackStackEntry?): ActionState {
    val savedState = backStackEntry?.savedStateHandle
    val noteId = savedState?.get<Long>("noteId")
    val actionType = savedState?.get<String>("actionType")?.let {
        NoteActionType.fromString(it)
    }

    return remember(noteId, actionType) { ActionState(noteId, actionType) }
}


fun NavHostController.popBackStackOnResume() {
    if (lifecycleState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private val NavHostController.lifecycleState: Lifecycle.State?
    get() = currentBackStackEntry?.lifecycle?.currentState

@Composable
fun DisablePredictiveBack(
    enabled: Boolean = true,
    onBackPress: () -> Unit
) {
    // Get the dispatcher and lifecycle owner
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner found"
    }.onBackPressedDispatcher

    val lifecycleOwner = LocalLifecycleOwner.current

    // Safely handle updates to the onBackPress lambda
    val currentOnBackPress = rememberUpdatedState(onBackPress)

    // Create the callback only once and update its enabled state
    val callback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBackPress.value()
            }
        }
    }

    // Update enabled state when the enabled parameter changes
    DisposableEffect(enabled, lifecycleOwner, callback) {
        callback.isEnabled = enabled
        backDispatcher.addCallback(lifecycleOwner, callback)

        onDispose {
            callback.remove()
        }
    }
}

/*@Composable
fun DisablePredictiveBack(
    activity: ComponentActivity,
    onBackPress: () -> Unit
) {
    DisposableEffect(activity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPress()
            }
        }

        activity.onBackPressedDispatcher.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }
}*/

