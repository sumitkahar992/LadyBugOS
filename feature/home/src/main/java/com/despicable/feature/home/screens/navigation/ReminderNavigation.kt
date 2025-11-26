package com.despicable.feature.home.screens.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.despicable.core.common.navigation.rememberActionState
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.home.screens.ReminderScreen
import kotlinx.serialization.Serializable

@Serializable
data object ReminderRoute

fun NavController.navigateToReminder(navOptions: NavOptions) = navigate(ReminderRoute, navOptions)


fun NavGraphBuilder.reminderScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    navigateToDetail: (id: Long) -> Unit
) {
    sharedElementComposable<ReminderRoute> {
        val actionState = rememberActionState(navController.currentBackStackEntry)
        ReminderScreen(
            onMenuClick = onMenuClick,
            navigateToNoteDetail = navigateToDetail,
            noteId = actionState.noteId,
            actionType = actionState.actionType,
            clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
        )
    }

}