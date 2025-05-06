package com.despicable.ladybugos

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import com.despicable.ladybugos.ui.MainContent
import dagger.hilt.android.AndroidEntryPoint
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainActivityViewModel: MainViewModel by viewModel()
//    private val editNoteViewModel: NoteDetailViewModel by viewModel()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()


        setContent {
            // If your deep link is note://com.despicable.ladybugos/notes/123
            val noteId = intent?.data?.pathSegments
                ?.takeIf { it.size > 1 && it[0] == "notes" }
                ?.get(1)
                ?.toLongOrNull() ?: -1L

            // Extract note ID and widget ID from intent
//            val noteId = remember { intent?.getLongExtra("noteId", -1L) } ?: -1L
            val widgetId = remember {
                intent?.let { safeIntent ->
                    val extraValue = safeIntent.getStringExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)
                    extraValue?.toIntOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
                } ?: AppWidgetManager.INVALID_APPWIDGET_ID
            }

            // If opened from widget, ensure proper activity flags
            if (noteId != -1L) {
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Initialize the NavController
            val navController = rememberNavController()

            val themeConfig by mainActivityViewModel.themeConfig.collectAsStateWithLifecycle()


            // Preload note data if opened from widget
            /*            LaunchedEffect(noteId, widgetId) {
                            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceAppWidgetManager = GlanceAppWidgetManager(this@MainActivity)
                                val glanceId = glanceAppWidgetManager.getGlanceIdBy(widgetId)
                                glanceId.let {
                                    editNoteViewModel.preloadNoteData(noteId, it)
                                }
                            }
                        }*/

            KoinAndroidContext {
                LadyBugOSTheme(themeConfig) {
                    Log.e("APP","MainActivity_[noteId]=[$noteId]")
                    Log.e("APP","MainActivity_[widgetId]=[$widgetId]")

                    PermissionContent {
                        MainContent(
                            navController = navController,
                            noteId = noteId
                        )
                    }
                }
            }




        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun PermissionContent(content: @Composable () -> Unit) {
        var showDialog by remember { mutableStateOf(false) }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    // Permission granted
                } else {
                    showDialog = true
                }
                Log.e("APP","Notification permission granted: $isGranted")
            }
        )

        LaunchedEffect(Unit) {
            checkAndRequestNotificationPermission(notificationPermissionLauncher)
        }

        NoteeDialog(
            enabled = showDialog,
            title = "Notification Permission",
            description = "We need notification permissions to remind you about your notes.",
            icon = Icons.Outlined.NotificationsActive,
            confirmText = "Open Settings",
            dismissText = "Dismiss",
            onConfirm = {
                showDialog = false
                openNotificationSettings()
            },
            onDismiss = { showDialog = false }
        )
        content()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestNotificationPermission(
        permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
            }

            else -> {
                // Request permission directly
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openNotificationSettings() {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).also {
            it.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(it)
        }
    }

}