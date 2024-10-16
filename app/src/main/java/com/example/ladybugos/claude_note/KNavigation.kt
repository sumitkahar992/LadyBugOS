package com.example.ladybugos.claude_note

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Navigation
sealed class KScreen(val route: String) {
    data object KNoteList : KScreen("noteList")
    data object KEditNote : KScreen("editNote?noteId={noteId}") {
        fun createRoute(noteId: Long?) = "editNote?noteId=$noteId"
    }
}

@Composable
fun KNoteNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = KScreen.KNoteList.route) {
        composable(KScreen.KNoteList.route) {
            KNoteListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(KScreen.KEditNote.createRoute(noteId))
                },
                onAddNoteClick = {
                    navController.navigate(KScreen.KEditNote.createRoute(null))
                }
            )
        }

        composable(
            route = KScreen.KEditNote.route,
            arguments = listOf(navArgument("noteId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")
            KEditNoteScreen(
                noteId = if (noteId != -1L) noteId else null,
                onSaveComplete = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()

                },

                )
        }
    }
}

// Koin Module
val kAppModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            KNoteDatabase::class.java,
            "note_database"
        ).build()
    }
    single { get<KNoteDatabase>().kNoteDao() }
    single { get<KNoteDatabase>().kTagDao() }

    // Repository
    single { KNoteRepository(get(), get()) }

    // Use Cases
    /*    factory { GetAllNotesUseCase(get()) }
        factory { GetNotesByTagUseCase(get()) }
        factory { GetAllTagsUseCase(get()) }
        factory { AddNoteUseCase(get()) }
        factory { UpdateNoteUseCase(get()) }
        factory { DeleteNoteUseCase(get()) }
        factory { AddTagUseCase(get()) }
        factory { UpdateTagUseCase(get()) }
        factory { DeleteTagUseCase(get()) }
        factory { GetNoteByIdUseCase(get()) }
        */

    // ViewModels
    viewModel { KNoteListViewModel(get()) }
    viewModel { KEditNoteViewModel(get(), get(), get()) }
}
