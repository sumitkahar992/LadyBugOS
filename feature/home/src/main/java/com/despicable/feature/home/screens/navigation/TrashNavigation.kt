package com.despicable.feature.home.screens.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.despicable.core.common.navigation.sharedElementComposable
import com.despicable.feature.home.screens.trash.TrashScreen
import kotlinx.serialization.Serializable


@Serializable
data object TrashRoute

fun NavController.navigateToTrash(navOptions: NavOptions) = navigate(TrashRoute, navOptions)


fun NavGraphBuilder.trashScreen(
    onMenuClick: () -> Unit,
    navigateToDetail: (id: Long) -> Unit
) {
    sharedElementComposable<TrashRoute> {
        TrashScreen(
            onMenuClick = onMenuClick,
            navigateToDetail = navigateToDetail,
        )
    }

}