package com.despicable.feature.home.screens.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.home.screens.LabelScreen
import kotlinx.serialization.Serializable


@Serializable
data object LabelRoute

fun NavController.navigateToLabel(navOptions: NavOptions) = navigate(LabelRoute, navOptions)


fun NavGraphBuilder.labelScreen(
    onNavigateUp: () -> Unit,
) {
    sharedElementComposable<LabelRoute> {
        LabelScreen(
            onNavigateBack = onNavigateUp
        )

    }

}