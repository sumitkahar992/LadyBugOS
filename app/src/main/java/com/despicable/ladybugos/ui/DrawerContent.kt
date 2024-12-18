package com.despicable.ladybugos.ui

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import com.despicable.feature.home.navigation.HomeRoute
import com.despicable.feature.home.navigation.navigateToHome
import com.despicable.feature.home.screens.navigation.ArchiveRoute
import com.despicable.feature.home.screens.navigation.LabelRoute
import com.despicable.feature.home.screens.navigation.ReminderRoute
import com.despicable.feature.home.screens.navigation.TrashRoute
import com.despicable.feature.home.screens.navigation.navigateToArchive
import com.despicable.feature.home.screens.navigation.navigateToLabel
import com.despicable.feature.home.screens.navigation.navigateToReminder
import com.despicable.feature.home.screens.navigation.navigateToTrash
import com.despicable.feature.settings.navigation.SettingsPage
import com.despicable.feature.settings.navigation.navigateToSettings
import com.despicable.ladybugos.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.reflect.KClass

@Composable
fun DrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
) {
    val coroutineScope = rememberCoroutineScope()


    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerShape = RoundedCornerShape(8.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(Modifier.height(14.dp))

        // Display App Logo or Title (e.g., Google Keep)
        DrawerHeader()

        // Optional Divider after App Title
        HDivider()


        DrawerItems(
            drawerState = drawerState,
            navController = navController,
            coroutineScope = coroutineScope
        )
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 28.dp)
    ) {
        Text(
            text = "Lost SouL",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun HDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    )
}

@Composable
private fun DrawerItems(
    drawerState: DrawerState,
    navController: NavHostController,
    coroutineScope: CoroutineScope
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination




    TopLevelDestination.entries.forEach { item ->
//        val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route) } == true

        val isSelected = currentDestination
            .isRouteInHierarchy(item.route)

        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        /*              val topLevelNavOptions = navOptions {
                                   // Pop up to the start destination of the graph to
                                   // avoid building up a large stack of destinations
                                   // on the back stack as users select items
                                   popUpTo(navController.graph.findStartDestination().id) {
                                       saveState = true
                                   }
                                   // Avoid multiple copies of the same destination when
                                   // reselecting the same item
                                   launchSingleTop = true
                                   // Restore state when reselecting a previously selected item
                                   restoreState = true
                               }
        */

        NavigationDrawerItem(
            icon = {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null
                )
            },
            label = {
                Text(
                    text = stringResource(id = item.label)
                )
            },
            selected = isSelected,
            onClick = {
                coroutineScope.launch {
                    Timber.tag("DEBUG").d("{ ROUTE } : ${item.route}")
                    Timber.tag("DEBUG").d("{ isSelected } : $isSelected")
                    Timber.tag("DEBUG")
                        .d("{ destination } : ${currentDestination?.hierarchy?.map { it.route }}}")
                    drawerState.close()
                    withContext(Dispatchers.Main) {
                        when (item) {
                            TopLevelDestination.HOME -> navController.navigateToHome(navOptions = topLevelNavOptions)
                            TopLevelDestination.REMINDER -> navController.navigateToReminder(
                                navOptions = topLevelNavOptions
                            )

                            TopLevelDestination.ARCHIVE -> navController.navigateToArchive(
                                navOptions = topLevelNavOptions
                            )

                            TopLevelDestination.LABELS -> navController.navigateToLabel(navOptions = topLevelNavOptions)
                            TopLevelDestination.TRASH -> navController.navigateToTrash(navOptions = topLevelNavOptions)
                            TopLevelDestination.SETTINGS -> navController.navigateToSettings(
                                navOptions = topLevelNavOptions
                            )

                            TopLevelDestination.HELP -> {}
                        }
                    }
                }
            },
            modifier = Modifier
                .width(280.dp)
                .padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false

@Keep
@Serializable
data object HelpAndFeedback

@Keep
enum class TopLevelDestination(
    val route: KClass<*>,
    val label: Int,
    val icon: Int
) {
    HOME(
        route = HomeRoute::class,
        label = R.string.drawer_notes,
        icon = R.drawable.notes
    ),
    REMINDER(
        route = ReminderRoute::class,
        label = R.string.drawer_reminders,
        icon = R.drawable.reminders
    ),
    ARCHIVE(
        route = ArchiveRoute::class,
        label = R.string.drawer_archive,
        icon = R.drawable.archive
    ),
    LABELS(
        route = LabelRoute::class,
        label = R.string.drawer_create_label,
        icon = R.drawable.add
    ),
    TRASH(
        route = TrashRoute::class,
        label = R.string.drawer_trash,
        icon = R.drawable.trash
    ),

    SETTINGS(
        route = SettingsPage::class,
        label = R.string.drawer_settings,
        icon = R.drawable.settings
    ),
    HELP(
        route = HelpAndFeedback::class,
        label = R.string.drawer_help_feedback,
        icon = R.drawable.help_feedback
    )
}


@PreviewLightDark
@Composable
fun DrawerContentPreview() {
    LadyBugOSTheme {
        DrawerContent(
            navController = rememberNavController(),
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
        )
    }
}

































