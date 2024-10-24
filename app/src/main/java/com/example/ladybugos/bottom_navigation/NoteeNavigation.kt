package com.example.ladybugos.bottom_navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@Composable
fun NoteeNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestinations: Destinations = Destinations.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestinations,
        modifier = modifier
    ) {

        composable<Destinations.Home> {
            HomeRoute()
        }

        composable<Destinations.Search> {
            SearchRoute()
        }

        composable<Destinations.Archive> {
            ArchiveRoute()
        }

        composable<Destinations.Trash> {
            TrashRoute()
        }

    }

}


@Composable
fun HomeRoute() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOME ROUTE",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun SearchRoute() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SEARCH ROUTE",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ArchiveRoute() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ARCHIVE ROUTE",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun TrashRoute() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TRASH ROUTE",
            style = MaterialTheme.typography.labelMedium
        )
    }
}











































