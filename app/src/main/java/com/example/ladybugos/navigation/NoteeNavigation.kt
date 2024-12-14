package com.example.ladybugos.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.ladybugos.ui.backup.BackupScreen
import com.example.ladybugos.ui.screens.ArchivedScreen
import com.example.ladybugos.ui.screens.LabelScreen
import com.example.ladybugos.ui.screens.ReminderScreen
import com.example.ladybugos.ui.screens.home.NoteListScreen
import com.example.ladybugos.ui.screens.note_detail.NoteDetailScreen
import com.example.ladybugos.ui.screens.settings.OSLicenseScreen
import com.example.ladybugos.ui.screens.settings.SettingsScreen
import com.example.ladybugos.ui.screens.trash.TrashScreen
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NoteeNavigation(
    navController: NavHostController,
    noteId: Long,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOpenedFromWidget = remember(noteId) { noteId != -1L }
    val startDestination = remember(noteId) {
        if (noteId == -1L) Screen.NoteList() else Screen.NoteDetail(noteId)
    }


    // Navigation actions
    val navigationActions = remember(navController) {
        NavigationActions(navController)
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // Note List Screen
                sharedElementComposable<Screen.NoteList> {
                    val actionState = rememberActionState(navController.currentBackStackEntry)
                    NoteListScreen(
                        navigateToDetail = navigationActions::navigateToNoteDetail,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        noteId = actionState.noteId,
                        actionType = actionState.actionType,
                        clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
                    )
                }

                // Note Detail Screen
                sharedElementComposable<Screen.NoteDetail> {
                    Timber.tag("DEBUG").d("isOpenedFromWidget : [$isOpenedFromWidget]")

                    NoteDetailScreen(
                        onBack = {
                            if (isOpenedFromWidget) {
                                navigationActions.handleWidgetNavigation(context)
                            } else {
                                navController.popBackStackOnResume()
                            }
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
                        navigateToNoteDetail = navigationActions::navigateToNoteDetail,
                        noteId = actionState.noteId,
                        actionType = actionState.actionType,
                        clearNoteAction = { actionState.clear(navController.currentBackStackEntry) }
                    )
                }

                sharedElementComposable<Screen.Labels> {
                    LabelScreen(
                        onNavigateBack = navController::popBackStackOnResume
                    )
                }

                sharedElementComposable<Screen.Trash> {
                    TrashScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        navigateToDetail = navigationActions::navigateToNoteDetail
                    )
                }



                sharedElementComposable<Screen.Reminders> {
                    val actionState = rememberActionState(navController.currentBackStackEntry)
                    ReminderScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        navigateToNoteDetail = navigationActions::navigateToNoteDetail,
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


// Separate class to handle navigation actions
private class NavigationActions(private val navController: NavHostController) {
    fun navigateToNoteDetail(noteId: Long) {
        navController.navigate(Screen.NoteDetail(id = noteId)) {
            launchSingleTop = true
        }
    }

    fun handleWidgetNavigation(context: Context) {
        if (navController.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(
                Lifecycle.State.CREATED
            ) == true
        ) {
            navController.navigate(Screen.NoteList()) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        } else {
            (context as? Activity)?.finish()
        }
    }
}



