package com.despicable.feature.detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.despicable.feature.detail.NoteDetailScreen
import kotlinx.serialization.Serializable


@Serializable
data class DetailRoute(val id: Long = -1L)

fun NavController.navigateToDetail(id: Long, navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = DetailRoute(id)) {
        navOptions()
    }
}


fun NavGraphBuilder.detailScreen(
    onBack: () -> Unit,
    onDelete: (id: Long) -> Unit,
    onArchive: (id: Long) -> Unit,
    onUnArchive: (id: Long) -> Unit
) {
    composable<DetailRoute> {
        NoteDetailScreen(
            onBack = onBack,
            onDelete = onDelete,
            onArchive = onArchive,
            onUnArchive = onUnArchive
        )
    }
}