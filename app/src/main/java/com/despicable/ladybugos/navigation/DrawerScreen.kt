package com.despicable.ladybugos.navigation

import kotlinx.serialization.Serializable



sealed interface Screen {

    @Serializable
    data class NoteList(
        val deletedId: Long? = null,
        val archivedId: Long? = null,
    ) : Screen

//    @Serializable
//    data class NoteDetail(val id: Long = -1L) : Screen

    @Serializable
    data object Reminders : Screen

    @Serializable
    data object Archive : Screen

    @Serializable
    data object Trash : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object BackupAndRestore : Screen

    @Serializable
    data object HelpAndFeedback : Screen


    @Serializable
    data object OSLicense : Screen

    @Serializable
    data object Labels : Screen



//    @Serializable
//    data object PrivacyPolicy : Screen

}



