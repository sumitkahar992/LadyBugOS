package com.despicable.ladybugos.navigation

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


                Timber.tag("DEBUG").d("isOpenedFromWidget : [$isOpenedFromWidget]")

                detailScreen(
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


private class NavigationActions(private val navController: NavHostController) {

    fun handleWidgetNavigation(context: Context) {
        if (navController.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(
                Lifecycle.State.CREATED
            ) == true
        ) {
            navController.navigate(HomeRoute()) {
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



