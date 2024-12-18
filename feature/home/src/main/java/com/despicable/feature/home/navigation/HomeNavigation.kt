package com.despicable.feature.home.navigation

import androidx.annotation.Keep
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.despicable.core.common.navigation.rememberActionState
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.home.NoteListScreen
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class HomeRoute(
    val deletedId: Long? = null,
    val archivedId: Long? = null,
)


fun NavController.navigateToHome(
    deletedId: Long? = null,
    archivedId: Long? = null,
    navOptions: NavOptions
) {
    navigate(route = HomeRoute(deletedId, archivedId), navOptions = navOptions)
}


fun NavGraphBuilder.homeScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    navigateToDetail: (id: Long) -> Unit
) {
    sharedElementComposable<HomeRoute> {
        val actionState = rememberActionState(navController.currentBackStackEntry)
        NoteListScreen(
            navigateToDetail = navigateToDetail,
            onMenuClick = onMenuClick,
            noteId = actionState.noteId,
            actionType = actionState.actionType,
            clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
        )
    }
}