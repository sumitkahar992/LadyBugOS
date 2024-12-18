package com.despicable.feature.backup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.backup.BackupScreen
import kotlinx.serialization.Serializable


@Serializable
data object BackupRoute

fun NavController.navigateToBackup(
    navOptions: NavOptions? = navOptions {
        launchSingleTop = true
    }
) = navigate(BackupRoute, navOptions)


fun NavGraphBuilder.backupScreen(
    onNavigateUp: () -> Unit
) {
    sharedElementComposable<BackupRoute> {
        BackupScreen(
            onNavigateUp = onNavigateUp
        )
    }

}