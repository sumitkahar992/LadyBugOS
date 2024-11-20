package com.example.ladybugos.ui.components

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
import com.example.ladybugos.R
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.ui.components.DrawerItem.Companion.DRAWER_DESTINATIONS
import com.example.ladybugos.ui.theme.LadyBugOSTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

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



    DRAWER_DESTINATIONS.forEach { item ->

        val isSelected =
            currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }


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
                    drawerState.close()
                    withContext(Dispatchers.Main) {
                        navController.navigate(
                            route = item.route,
                            navOptions = topLevelNavOptions
                        )
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

@Serializable
data class DrawerItem(
    val route: Screen,
    val label: Int,
    val icon: Int
) {
    companion object {
        val DRAWER_DESTINATIONS = listOf(
            DrawerItem(
                route = Screen.NoteList(),
                label = R.string.drawer_notes,
                icon = R.drawable.notes
            ),
            DrawerItem(
                route = Screen.Reminders,
                label = R.string.drawer_reminders,
                icon = R.drawable.reminders
            ),
            DrawerItem(
                route = Screen.Archive,
                label = R.string.drawer_archive,
                icon = R.drawable.archive
            ),
            DrawerItem(
                route = Screen.Labels,
                label = R.string.drawer_create_label,
                icon = R.drawable.add
            ),
            DrawerItem(
                route = Screen.Trash,
                label = R.string.drawer_trash,
                icon = R.drawable.trash
            ),
            DrawerItem(
                route = Screen.Settings,
                label = R.string.drawer_settings,
                icon = R.drawable.settings
            ),
            DrawerItem(
                route = Screen.HelpAndFeedback,
                label = R.string.drawer_help_feedback,
                icon = R.drawable.help_feedback
            )
        )
    }
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

































