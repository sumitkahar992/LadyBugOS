package com.despicable.ladybugos.ui

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
import com.despicable.core.common.navigation.DisablePredictiveBack
import com.despicable.feature.backup.navigation.BackupRoute
import com.despicable.feature.detail.navigation.DetailRoute
import com.despicable.feature.home.navigation.HomeRoute
import com.despicable.feature.home.screens.navigation.ArchiveRoute
import com.despicable.feature.home.screens.navigation.LabelRoute
import com.despicable.feature.home.screens.navigation.ReminderRoute
import com.despicable.feature.home.screens.navigation.TrashRoute
import com.despicable.feature.settings.navigation.OSLicense
import com.despicable.feature.settings.navigation.SettingsPage
import com.despicable.ladybugos.navigation.NoteeNavigation
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
            it.hasRoute(DetailRoute::class)
                    || it.hasRoute(BackupRoute::class) ||
                    it.hasRoute(OSLicense::class)
        } == true

    }

    // Determine if current screen is a drawer screen using KClass route checking
    val isDrawerScreen = remember(currentDestination) {
        currentDestination?.hierarchy?.any {
            it.hasRoute(ArchiveRoute::class) ||
                    it.hasRoute(LabelRoute::class) ||
                    it.hasRoute(TrashRoute::class) ||
                    it.hasRoute(ReminderRoute::class)
                    || it.hasRoute(SettingsPage::class)
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
                    navController.popBackStack(HomeRoute(), false)
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


















































