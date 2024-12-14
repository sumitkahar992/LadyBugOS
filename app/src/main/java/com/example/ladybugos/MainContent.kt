package com.example.ladybugos

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ladybugos.navigation.DisablePredictiveBack
import com.example.ladybugos.navigation.NoteeNavigation
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.ui.components.DrawerContent
import kotlinx.coroutines.launch

@Composable
fun MainContent(
    navController: NavHostController,
    noteId: Long
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    val isSelectedNoteDetail = remember(currentDestination) {
        currentDestination?.hierarchy?.any {
            it.hasRoute(Screen.NoteDetail::class) ||
                    it.hasRoute(Screen.BackupAndRestore::class) ||
                    it.hasRoute(Screen.OSLicense::class)
        } == true

    }

    // Determine if current screen is a drawer screen using KClass route checking
    val isDrawerScreen = remember(currentDestination) {
        currentDestination?.hierarchy?.any {
            it.hasRoute(Screen.Archive::class) ||
                    it.hasRoute(Screen.Labels::class) ||
                    it.hasRoute(Screen.Trash::class) ||
                    it.hasRoute(Screen.Reminders::class) ||
                    it.hasRoute(Screen.Settings::class)
        } == true
    }

    // Handle back press based on screen type
    DisablePredictiveBack(
        enabled = isDrawerScreen || drawerState.isOpen,
        onBackPress = {
            when {
                drawerState.isOpen -> {
                    scope.launch { drawerState.close() }
                }

                isDrawerScreen -> {
                    navController.popBackStack(Screen.NoteList(), false)
                }
            }
        }
    )


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                drawerState = drawerState
            )
        },
        gesturesEnabled = !isSelectedNoteDetail
    ) {

        NoteeNavigation(
            navController = navController,
            noteId = noteId,
            drawerState = drawerState
        )
    }
}


















































