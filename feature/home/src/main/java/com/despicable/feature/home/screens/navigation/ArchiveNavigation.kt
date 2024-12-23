package com.despicable.feature.home.screens.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.despicable.core.common.navigation.rememberActionState
import com.despicable.core.common.navigation.sharedElementComposable

import com.despicable.feature.home.screens.ArchivedScreen
import kotlinx.serialization.Serializable

@Serializable
data object ArchiveRoute

fun NavController.navigateToArchive(navOptions: NavOptions) = navigate(ArchiveRoute, navOptions)


fun NavGraphBuilder.archiveScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    navigateToDetail: (id: Long) -> Unit
) {
    sharedElementComposable<ArchiveRoute> {
        val actionState = rememberActionState(navController.currentBackStackEntry)
        ArchivedScreen(
            onMenuClick = onMenuClick,
            navigateToNoteDetail = navigateToDetail,
            noteId = actionState.noteId,
            actionType = actionState.actionType,
            clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
        )
    }

}