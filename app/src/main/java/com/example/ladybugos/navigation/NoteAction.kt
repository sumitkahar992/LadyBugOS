package com.example.ladybugos.navigation

import androidx.navigation.NavController

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


