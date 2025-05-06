package com.despicable.ladybugos.navigation

import android.app.Activity
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.despicable.core.common.navigation.LocalSharedTransitionScope
import com.despicable.core.common.navigation.NoteAction
import com.despicable.core.common.navigation.handleAction
import com.despicable.core.common.navigation.popBackStackOnResume
import com.despicable.feature.backup.navigation.navigateToBackup
import com.despicable.feature.detail.navigation.DetailRoute
import com.despicable.feature.detail.navigation.detailScreen
import com.despicable.feature.detail.navigation.navigateToDetail
import com.despicable.feature.home.navigation.HomeRoute
import com.despicable.feature.home.navigation.homeScreen
import com.despicable.feature.home.screens.navigation.archiveScreen
import com.despicable.feature.home.screens.navigation.labelScreen
import com.despicable.feature.home.screens.navigation.reminderScreen
import com.despicable.feature.home.screens.navigation.trashScreen
import com.despicable.feature.settings.navigation.OSLicense
import com.despicable.feature.settings.navigation.settingsScreen
import kotlinx.coroutines.launch


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NoteeNavigation(
    navController: NavHostController,
    noteId: Long,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
// Track if the app was opened from a widget, persists across config changes
    var isOpenedFromWidget by rememberSaveable { mutableStateOf(noteId != -1L) }
    val startDestination = remember(noteId) {
        if (noteId == -1L) HomeRoute() else DetailRoute(noteId)
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

                homeScreen(
                    navController = navController,
                    navigateToDetail = navController::navigateToDetail,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )


                Log.e("APP","isOpenedFromWidget : [$isOpenedFromWidget]")

                detailScreen(
                    onBack = { isFromTopBar ->
                        when {
                            isOpenedFromWidget && isFromTopBar -> {
                                navigationActions.handleWidgetNavigation()
                                isOpenedFromWidget = false
                            }
                            isOpenedFromWidget -> {  // Implicitly && !isFromTopBar
                                (navController.context as? Activity)?.finish()
                            }
                            else -> {
                                navController.navigateUp()
                            }
                        }
                    },
                    onDelete = { noteId -> navController.handleAction(NoteAction.Delete(noteId)) },
                    onArchive = { noteId -> navController.handleAction(NoteAction.Archive(noteId)) },
                    onUnArchive = { noteId -> navController.handleAction(NoteAction.Unarchive(noteId)) }
                )



                archiveScreen(
                    navController = navController,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    navigateToDetail = navController::navigateToDetail,
                )


                labelScreen(
                    onNavigateUp = navController::popBackStackOnResume
                )


                trashScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    navigateToDetail = navController::navigateToDetail
                )




                reminderScreen(
                    navController = navController,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    navigateToDetail = navController::navigateToDetail,
                )


                settingsScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onPrivacyClick = {},
                    onLicenseClick = { navController.navigate(OSLicense) },
                    onBackUpClick = { navController.navigateToBackup() },
                    onNavigateUp = navController::popBackStackOnResume
                )
            }
        }
    }
}


// Helper class for navigation actions
private class NavigationActions(private val navController: NavHostController) {
    fun handleWidgetNavigation() {
        navController.navigate(HomeRoute()) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}



