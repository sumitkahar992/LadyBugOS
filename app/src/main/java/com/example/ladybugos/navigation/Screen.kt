package com.example.ladybugos.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable


@Keep
@Serializable
sealed class Screen {

    companion object {
        fun getAllItems() = listOf(
            NoteList(),
            Archive,
            Reminders,
            CreateNewLabel,
            Trash,
            Settings,
            HelpAndFeedback
        )
    }


    @Serializable
    data class NoteList(val deletedId: Long? = null) : Screen() {
        val name = "NoteList"
    }

    @Serializable
    data class NoteDetail(val id: Long = -1L, val reminderAction: String? = null) : Screen() {
        val name = "NoteDetail"
    }

    @Serializable
    data object Reminders : Screen() {
        val name = "Reminders"
    }

    @Serializable
    data object CreateNewLabel : Screen() {
        val name = "CreateNewLabel"
    }

    @Serializable
    data object Archive : Screen() {
        val name = "Archive"
    }

    @Serializable
    data object Trash : Screen() {
        val name = "Trash"
    }

    @Serializable
    data object Settings : Screen() {
        const val name = "Settings"
    }

    @Serializable
    data object HelpAndFeedback : Screen() {
        val name = "HelpAndFeedback"
    }

}




