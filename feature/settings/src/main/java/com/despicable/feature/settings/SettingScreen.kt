package com.despicable.feature.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.despicable.core.designsystem.component.NoteeDialog
import com.despicable.core.designsystem.theme.LocalThemeProvider
import com.despicable.core.designsystem.theme.Theme
import org.koin.androidx.compose.koinViewModel

fun requestScheduleExactAlarmIntent(context: Context): Intent {
    return Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM").apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onMenuClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onBackUpClick: () -> Unit,
) {
    var blackTheme by remember { mutableStateOf(false) }
    var biometricLock by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showThemeDialog by remember { mutableStateOf(false) }

    val theme = LocalThemeProvider.theme
    val materialYou = LocalThemeProvider.dynamicColor

    val context = LocalContext.current
    val alarmManager: AlarmManager = remember { requireNotNull(context.getSystemService()) }

    var canScheduleExactAlarms by remember {
        mutableStateOf(AlarmManagerCompat.canScheduleExactAlarms(alarmManager))
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canScheduleExactAlarms = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
        }

    var showExactAlarmsDialog by remember {
        mutableStateOf(false)
    }


    Scaffold(
        topBar = {

            LargeTopAppBar(
                title = { Text("Settings", Modifier.padding(start = 6.dp)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Display") {
                SettingsItem(
                    title = "Default Theme",
                    subtitle = theme.name,
                    icon = Icons.Default.DarkMode,
                    onClick = {
                        // Open theme selection dialog
                        showThemeDialog = true
                    }
                )
                SettingsSwitch(
                    title = "Black Theme",
                    subtitle = "Enable black AMOLED theme",
                    icon = Icons.Default.Contrast,
                    checked = blackTheme,
                    onCheckedChange = { blackTheme = it }
                )
                SettingsSwitch(
                    title = "Material You",
                    subtitle = "Switch theme according to your wallpaper (A12+ Only)",
                    icon = Icons.Default.Palette,
                    checked = materialYou,
                    onCheckedChange = { viewModel.updateDynamicColor(it) }
                )
                SettingsItem(
                    title = "Goal Card Style",
                    subtitle = "Compact",
                    icon = Icons.Default.ViewAgenda,
                    onClick = {
                        // Open goal card style selection
                    }
                )
                AnimatedVisibility(visible = !canScheduleExactAlarms) {
                    SettingsItem(
                        title = "Grant exact alarm permission",
                        subtitle = "Allow this app to set reminder for notes",
                        icon = Icons.Default.Alarm,
                        onClick = {
                            showExactAlarmsDialog = true
                        }
                    )
                }
            }

            SettingsSection(title = "Locales") {
                SettingsItem(
                    title = "Default Locale",
                    subtitle = "Select your preferred locale",
                    icon = Icons.Default.Language,
                    onClick = {
                        // Open locale selection
                    }
                )
                SettingsItem(
                    title = "Date Format",
                    subtitle = "DD/MM/YYYY",
                    icon = Icons.Default.DateRange,
                    onClick = {
                        // Open date format selection
                    }
                )
                SettingsItem(
                    title = "Preferred Currency",
                    subtitle = "US Dollar ($)",
                    icon = Icons.Default.AttachMoney,
                    onClick = {
                        // Open currency selection
                    }
                )
            }

            SettingsSection(title = "Security") {
                SettingsSwitch(
                    title = "Biometric Lock",
                    subtitle = "Use your fingerprint or screen lock password to unlock GreenStash",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricLock,
                    onCheckedChange = { biometricLock = it }
                )

                SettingsItem(
                    title = "BackUp",
                    subtitle = "Back up your notes data to a secure location. So that, you can restore them later",
                    icon = Icons.Default.CloudQueue,
                    onClick = onBackUpClick
                )
            }



            SettingsSection(title = "Miscellaneous") {
                SettingsItem(
                    title = "License & Acknowledgement",
                    subtitle = "Show open source license information.",
                    icon = Icons.Default.Info,
                    onClick = onLicenseClick
                )
                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "Click to view our privacy policy",
                    icon = Icons.Default.Info,
                    onClick = onPrivacyClick
                )
            }
        }
    }


    NoteeDialog(
        enabled = showExactAlarmsDialog,
        title = "Grant Exact Alarm Permission",
        onDismiss = { showExactAlarmsDialog = false },
        description = {
            Text(
                "Allow this app to set alarms and schedule time-sensitive actions. This lets the app run in the background, which may use more battery.\n" +
                        "\n" +
                        "If this permission is off, existing alarms and time-based events scheduled by this app won't work."
            )
        },
        confirmText = "Grant",
        dismissText = "dismiss",
        onConfirm = {
            showExactAlarmsDialog = false
            launcher.launch(requestScheduleExactAlarmIntent(context))
        },
    )

    NoteeDialog(
        enabled = showThemeDialog,
        title = "Choose Theme",
        onDismiss = { showThemeDialog = false },
        description = {
            Column(Modifier.selectableGroup()) {
                ThemeOption(Theme.Light, theme) {
                    viewModel.updateTheme(Theme.Light)
                    showThemeDialog = false
                }
                ThemeOption(Theme.Dark, theme) {
                    viewModel.updateTheme(Theme.Dark)
                    showThemeDialog = false
                }
                ThemeOption(Theme.System, theme) {
                    viewModel.updateTheme(Theme.System)
                    showThemeDialog = false
                }
            }
        }
    )
}


@Composable
fun ThemeOption(theme: Theme, selectedTheme: Theme, onSelectTheme: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(43.dp)
            .selectable(
                selected = (theme == selectedTheme),
                onClick = onSelectTheme,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (theme == selectedTheme),
            onClick = null  // null here because the parent selectable will handle the click
        )
        Text(
            text = theme.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}