package com.example.ladybugos.navigation

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.ladybugos.ui.backup.BackupScreen
import com.example.ladybugos.ui.drawer.ArchivedScreen
import com.example.ladybugos.ui.drawer.ReminderScreen
import com.example.ladybugos.ui.drawer.home.CreateNewLabelScreen
import com.example.ladybugos.ui.drawer.home.NoteListScreen
import com.example.ladybugos.ui.drawer.note_detail.NoteDetailScreen
import com.example.ladybugos.ui.drawer.settings.OSLicenseScreen
import com.example.ladybugos.ui.drawer.settings.SettingsScreen
import com.example.ladybugos.ui.drawer.trash.TrashScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NoteeNavigation(
    navController: NavHostController,
    noteId: Long,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()

    fun navigateToNoteDetail(noteId: Long) {
        navController.navigate(Screen.NoteDetail(id = noteId)) { launchSingleTop = true }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = remember(noteId) {
                    if (noteId == -1L) Screen.NoteList() else Screen.NoteDetail(noteId)
                }
            ) {
                // Note List Screen
                sharedElementComposable<Screen.NoteList> {
                    val actionState = rememberActionState(navController.currentBackStackEntry)
                    NoteListScreen(
                        navigateToDetail = ::navigateToNoteDetail,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        noteId = actionState.noteId,
                        actionType = actionState.actionType,
                        clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
                    )
                }

                // Note Detail Screen
                sharedElementComposable<Screen.NoteDetail> {
                    val activity = LocalContext.current as? Activity
                    val isOpenedFromWidget =
                        activity?.intent?.getBooleanExtra("fromWidget", false) ?: false

                    NoteDetailScreen(
                        onBack = {
                            if (isOpenedFromWidget) activity?.finish()
                            else navController.popBackStackOnResume()
                        },
                        onDelete = { noteId ->
                            navController.handleAction(NoteAction.Delete(noteId))
                        },
                        onArchive = { noteId ->
                            navController.handleAction(NoteAction.Archive(noteId))
                        },
                        onUnArchive = { noteId ->
                            navController.handleAction(NoteAction.Unarchive(noteId))
                        })
                }

                sharedElementComposable<Screen.Archive> {
                    val actionState = rememberActionState(navController.currentBackStackEntry)
                    ArchivedScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        navigateToNoteDetail = ::navigateToNoteDetail,
                        noteId = actionState.noteId,
                        actionType = actionState.actionType,
                        clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
                    )
                }

                sharedElementComposable<Screen.Labels> {
                    CreateNewLabelScreen(
                        onNavigateBack = navController::popBackStackOnResume
                    )
                }

                sharedElementComposable<Screen.Trash> {
                    TrashScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        navigateToNoteDetail = ::navigateToNoteDetail
                    )
                }



                sharedElementComposable<Screen.Reminders> {
                    val actionState = rememberActionState(navController.currentBackStackEntry)
                    ReminderScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        navigateToNoteDetail = ::navigateToNoteDetail,
                        noteId = actionState.noteId,
                        actionType = actionState.actionType,
                        clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
                    )
                }

                sharedElementComposable<Screen.Settings> {
                    SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onPrivacyClick = {},
                        onOSLicenseClick = { navController.navigate(Screen.OSLicense) },
                        onBackUpClick = { navController.navigate(Screen.BackupAndRestore) }
                    )
                }

                sharedElementComposable<Screen.BackupAndRestore> {
                    BackupScreen(
                        onNavigateUp = navController::popBackStackOnResume
                    )
                }

                sharedElementComposable<Screen.OSLicense> {
                    OSLicenseScreen(
                        onBackPress = navController::popBackStackOnResume
                    )
                }
            }
        }
    }
}

/*
   Manages the state of note actions (delete, archive, unarchive)
*/
@Stable
private class ActionState(
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
private fun rememberActionState(backStackEntry: NavBackStackEntry?): ActionState {
    val savedState = backStackEntry?.savedStateHandle
    val noteId = savedState?.get<Long>("noteId")
    val actionType = savedState?.get<String>("actionType")?.let {
        NoteActionType.fromString(it)
    }

    return remember(noteId, actionType) { ActionState(noteId, actionType) }
}


private fun NavHostController.popBackStackOnResume() {
    if (lifecycleState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private val NavHostController.lifecycleState: Lifecycle.State?
    get() = currentBackStackEntry?.lifecycle?.currentState

