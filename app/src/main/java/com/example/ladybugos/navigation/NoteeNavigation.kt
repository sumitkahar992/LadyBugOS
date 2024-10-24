package com.example.ladybugos.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ladybugos.ui.drawer.ArchivedScreen
import com.example.ladybugos.ui.drawer.ReminderScreen
import com.example.ladybugos.ui.drawer.home.NoteListScreen
import com.example.ladybugos.ui.drawer.note_detail.NoteDetailScreen
import com.example.ladybugos.ui.drawer.settings.SettingsScreen
import com.example.ladybugos.ui.drawer.trash.TrashScreen
import kotlinx.coroutines.launch

@Composable
fun NoteeNavigation(
    navController: NavHostController,
    noteId: Long,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()

    fun navigateToNoteDetail(noteId: Long) {
        navController.navigate(Screen.NoteDetail(id = noteId)) {
            launchSingleTop = true
        }
    }

    val startDestination = remember(noteId) {
        if (noteId == -1L) Screen.NoteList() else Screen.NoteDetail(noteId)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.NoteList> {
            NoteListScreen(
                navigateToNoteDetail = ::navigateToNoteDetail,
                onMenuClick = { scope.launch { drawerState.open() } },
            )
        }

        composable<Screen.NoteDetail> {

//            val noteID = backStackEntry.arguments?.getLong("noteId") ?: -1L


            NoteDetailScreen(
                onBack = navController::popBackStackOnResume,
                onDelete = { deletedId ->
                    navController.navigate(Screen.NoteList(deletedId = deletedId)) {
                        popUpTo<Screen.NoteList> { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.Archive> {
            ArchivedScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail,
            )
        }

        composable<Screen.Trash> {
            TrashScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail,
            )
        }



        composable<Screen.Reminders> {
            ReminderScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
                navigateToNoteDetail = ::navigateToNoteDetail
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onMenuClick = { scope.launch { drawerState.open() } },
            )
        }


    }
}


private fun NavHostController.popBackStackOnResume() {
    if (lifecycleState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private val NavHostController.lifecycleState: Lifecycle.State?
    get() = currentBackStackEntry?.lifecycle?.currentState

/*
  Timber.tag("DEBUG").d("id : ${backStackEntry.arguments?.getLong("id")}")
       Timber.tag("DEBUG").d("backStackEntry : ${backStackEntry.destination.route}")

       val noteScreenArgs: Screen.NoteDetail = backStackEntry.toRoute()
       Timber.tag("DEBUG").d("noteScreenArgs[$noteScreenArgs]")
 */
