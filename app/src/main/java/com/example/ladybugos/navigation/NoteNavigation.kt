package com.example.ladybugos.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

/*@Composable
fun NoteeNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    noteId: Long,
) {
    val startDestination = remember(noteId) {
        if (noteId == -1L) Screen.NoteList() else Screen.NoteDetail(noteId)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.NoteList> {
            *//*    HomeScreen(
                    onNavigateToEdit = { note ->
                        Timber.tag("DEBUG").d("EDIT_[${note.id}]")
                        navController.navigate(Screen.NoteDetail(id = note.id)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.NoteDetail(id = -1L))
                    },
                    navController = navController,

                    )*//*
        }

        composable<Screen.NoteDetail> { _ ->
            EditNoteScreen(
                onBack = navController::popBackStackOnResume,
                onDelete = { deletedId ->
                    Timber.tag("DEBUG").d("deletedId[$deletedId]")


                    navController.navigate(Screen.NoteList(deletedId = deletedId)) {
                        popUpTo<Screen.NoteList> { inclusive = true }
                    }

                },
            )
        }


        composable<Screen.Reminders> {
            ReminderScreen(
                onSearch = {},
                topBarContent = {}
            )
        }
        composable<Screen.CreateNewLabel> {

        }
        composable<Screen.Archive> {
            ArchiveScreen(
                notes = emptyList(),
                onSearch = {},
                navigateToEdit = {},
                selectedTheme = Theme.LIGHT,
            )
        }
        composable<Screen.Trash> {
            TrashScreen(
                onSearch = {}
            )
        }
        composable<Screen.Settings> {
            SettingScreen(
                onSearch = {}
            )
        }
        composable<Screen.HelpAndFeedback> {

        }
    }
}*/


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
