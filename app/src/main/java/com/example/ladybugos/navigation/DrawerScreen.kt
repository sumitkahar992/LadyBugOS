package com.example.ladybugos.navigation

import kotlinx.serialization.Serializable


sealed interface Screen {

    @Serializable
    data class NoteList(val deletedId: Long? = null) : Screen

    @Serializable
    data class NoteDetail(val id: Long = -1L) : Screen

    @Serializable
    data object Reminders : Screen

    @Serializable
    data object Archive : Screen

    @Serializable
    data object Trash : Screen

    @Serializable
    data object Settings : Screen


    @Serializable
    data object HelpAndFeedback : Screen

}



