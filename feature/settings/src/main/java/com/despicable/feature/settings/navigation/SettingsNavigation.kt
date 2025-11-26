package com.despicable.feature.settings.navigation

import androidx.annotation.Keep
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.backup.navigation.backupScreen
import com.despicable.feature.settings.OSLicenseScreen
import com.despicable.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable


@Keep
@Serializable
data object SettingsRoute

@Keep
@Serializable
data object SettingsPage


@Serializable
data object HelpAndFeedback

@Serializable
data object OSLicense


fun NavController.navigateToSettings(navOptions: NavOptions) =
    navigate(route = SettingsRoute, navOptions)

fun NavGraphBuilder.settingsScreen(
    onMenuClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onBackUpClick: () -> Unit,
    onNavigateUp: () -> Unit
) {
    navigation<SettingsPage>(startDestination = SettingsRoute) {
        sharedElementComposable<SettingsRoute> {
            SettingsScreen(
                onMenuClick = onMenuClick,
                onPrivacyClick = onPrivacyClick,
                onLicenseClick = onLicenseClick,
                onBackUpClick = onBackUpClick
            )
        }


        backupScreen(
            onNavigateUp = onNavigateUp
        )


        sharedElementComposable<OSLicense> {
            OSLicenseScreen(
                onBackPress = onNavigateUp
            )
        }
    }

}