package com.example.ladybugos.ui.drawer.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSLicenseScreen(onBackPress: () -> Unit) {

    val view = LocalView.current
    val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehaviour.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Open Source Libraries") },
                navigationIcon = {
                    // weak haptic feedback
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    IconButton(
                        onClick = onBackPress
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehaviour
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { pv ->

        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv),
            showAuthor = true,
            showVersion = true,
            showLicenseBadges = true,
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onBackground,
                badgeBackgroundColor = MaterialTheme.colorScheme.primary,
                badgeContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

    }
}


@Preview
@Composable
private fun OSLPreview() {
    OSLicenseScreen {}
}
