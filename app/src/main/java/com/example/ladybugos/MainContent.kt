package com.example.ladybugos

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ladybugos.navigation.NoteeNavigation
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.ui.components.DrawerContent

@Composable
fun MainContent(
    navController: NavHostController,
    noteId: Long
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    val isSelectedNoteDetail =
        currentDestination?.hierarchy?.any { it.hasRoute(Screen.NoteDetail::class) } == true

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
            drawerState = drawerState,
            noteId = noteId
        )
    }
}