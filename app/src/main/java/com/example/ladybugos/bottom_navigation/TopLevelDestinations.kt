package com.example.ladybugos.bottom_navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable


sealed interface Destinations {

    @Serializable
    data object Home : Destinations

    @Serializable
    data object Search : Destinations

    @Serializable
    data object Archive : Destinations

    @Serializable
    data object Trash : Destinations

     @Serializable
    data object Settings : Destinations

}


enum class TopLevelDestinations(
    val label : String,
    val icon: ImageVector,
    val route: Destinations
) {
    HOME(
        label = "Home",
        icon = Icons.Outlined.Lightbulb,
        route = Destinations.Home
    ),
    REMINDERS(
        label = "Search",
        icon = Icons.Outlined.Search,
        route = Destinations.Search
    ),
    ARCHIVE(
        label = "Archive",
        icon = Icons.Outlined.Archive,
        route = Destinations.Archive
    ),
    TRASH(
        label = "Trash",
        icon = Icons.Outlined.DeleteOutline,
        route = Destinations.Trash
    ),

}































































