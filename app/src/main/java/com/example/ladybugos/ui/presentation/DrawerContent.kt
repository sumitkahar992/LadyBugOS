package com.example.ladybugos.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.ladybugos.navigation.Screen
import com.example.ladybugos.ui.theme.LadyBugOSTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
private fun DrawerItems(
    items: List<DrawerItem>,
    selectedItem: MutableState<DrawerItem>,
    drawerState: DrawerState,
    navController: NavController,
    coroutineScope: CoroutineScope
) {
    items.forEach { item ->
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null
                )
            },
            label = {
                Text(
                    text = item.label
                )
            },
            selected = item == selectedItem.value,
            onClick = {
                val topLevelNavOptions = navOptions {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }

                selectedItem.value = item
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

data class DrawerItem(
    val route: Screen,
    val label: String,
    val icon: ImageVector
)


fun getDrawerItems(): List<DrawerItem> {
    return listOf(
        DrawerItem(route = Screen.NoteList(), label = "Notes", icon = Icons.Outlined.Lightbulb),
        DrawerItem(
            route = Screen.Reminders,
            label = "Reminders",
            icon = Icons.Outlined.Notifications
        ),
        DrawerItem(
            route = Screen.CreateNewLabel,
            label = "Create New Label",
            icon = Icons.Default.Add
        ),
        DrawerItem(route = Screen.Archive, label = "Archive", icon = Icons.Outlined.Archive),
        DrawerItem(route = Screen.Trash, label = "Trash", icon = Icons.Outlined.Delete),
        DrawerItem(route = Screen.Settings, label = "Settings", icon = Icons.Outlined.Settings),
        DrawerItem(
            route = Screen.HelpAndFeedback,
            label = "Help & Feedback",
            icon = Icons.AutoMirrored.Outlined.HelpOutline
        )
    )
}


@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    selectedItem: MutableState<DrawerItem>,
) {

    val drawerItems =
        getDrawerItems() // Retrieve the list of drawer items    val selectedItem = remember { mutableStateOf(items[0]) }
    val coroutineScope = rememberCoroutineScope()


    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerShape = RoundedCornerShape(8.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(Modifier.height(14.dp))

        // Display App Logo or Title (e.g., Google Keep)
        Text(
            text = "Lost SouL",
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            fontSize = 33.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Optional Divider after App Title
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )


        DrawerItems(
            items = drawerItems,
            selectedItem = selectedItem,
            drawerState = drawerState,
            navController = navController,
            coroutineScope = coroutineScope
        )


//        // Display drawer items based on the screens
//        menuItems.forEach { (screen, displayName, icon) ->
//            DrawerMenuItem(
//                text = displayName, // Use the displayName for the menu item text
//                icon = icon,
//                onClick = { onMenuItemClick(screen.toString()) },
//                isSelected = screen.toString() == selectedRoute
//            )
//        }
    }
}

@Composable
fun DrawerMenuItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean, // Parameter to determine if the menu item is selected
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    // Menu item content with rounded corners, padding, and dynamic colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp) // Add padding
            .background(
                backgroundColor,
                shape = RoundedCornerShape(12.dp)
            ) // Rounded corners and dynamic background
            .padding(horizontal = 16.dp, vertical = 12.dp), // Inner padding for the content
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor
        )
        Spacer(Modifier.width(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}


fun getGreeting(): String {
    val currentTime = System.currentTimeMillis()
    val simpleDateFormat = SimpleDateFormat("HH", Locale.US)

    return when (simpleDateFormat.format(Date(currentTime)).toInt()) {
        in 0..11 -> "Good Morning Boss !"
        in 12..16 -> "Good Afternoon Boss !"
        in 17..20 -> "Good Evening Boss !"
        else -> "Good Night!"
    }
}


@PreviewLightDark
@Composable
fun DrawerContentPreview() {
    LadyBugOSTheme {
        DrawerContent(
            navController = rememberNavController(),
            drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
            selectedItem = remember {
                mutableStateOf(
                    DrawerItem(
                        Screen.NoteList(),
                        "Notes",
                        Icons.Outlined.Lightbulb
                    )
                )
            },

            )
    }
}
































