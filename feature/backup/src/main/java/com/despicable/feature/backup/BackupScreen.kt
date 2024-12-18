package com.despicable.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import com.despicable.core.database.model.BackupResults
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = koinViewModel(),
    onNavigateUp: () -> Unit
) {
    rememberCoroutineScope()
    val backupState by viewModel.backupState.collectAsState()

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.performBackup(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.performRestore(it) } }

    val generateBackupFileName = remember {
        {
            val timestamp =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            "note_backup-$timestamp.zip"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup data", Modifier.padding(start = 12.dp)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateUp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }

                }
            )
        }
    ) { pv ->
        BackupScreenContent(
            modifier = Modifier.padding(paddingValues = pv),
            backupState = backupState,
            onBackupClicked = { backupLauncher.launch(generateBackupFileName()) },
            onRestoreClicked = { restoreLauncher.launch(arrayOf("application/zip")) }
        )
    }


}

@Composable
private fun BackupScreenContent(
    modifier: Modifier = Modifier,
    backupState: BackupResults?,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionButton(
            modifier = Modifier.weight(0.5f),
            onBackupClicked = onBackupClicked,
            onRestoreClicked = onRestoreClicked
        )

        Box(modifier = Modifier.weight(0.5f)) {
            backupState?.let { state ->
                when (state) {
                    is BackupResults.Progress -> ProgressView(state)
                    is BackupResults.Success -> SuccessView(state)
                    is BackupResults.Error -> ErrorView(state)

                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onBackupClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("Backup Notes", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRestoreClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("Restore Notes", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ErrorView(backupState: BackupResults.Error) {
    Text(
        text = "Error: ${backupState.message}",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SuccessView(backupState: BackupResults.Success) {
    val operationText = when (backupState.operation) {
        BackupResults.Success.Operation.BACKUP -> "Backup"
        BackupResults.Success.Operation.RESTORE -> "Restore"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$operationText completed successfully",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )

        StatusChip(
            icon = Icons.AutoMirrored.Outlined.Note,
            count = backupState.notesCount,
            label = " Notes"
        )
        StatusChip(
            icon = Icons.Outlined.Tag,
            count = backupState.tagsCount,
            label = " Tags"
        )
    }
}

@Composable
private fun ProgressView(backupState: BackupResults.Progress) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { backupState.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )
        Text(
            text = "${backupState.percentage}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun StatusChip(
    icon: ImageVector,
    count: Int,
    label: String
) {
    Surface(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@PreviewLightDark
@Composable
fun BackupContentPreviews(
    @PreviewParameter(BackupContentPreviewProvider::class)
    backupState: BackupResults
) {
    LadyBugOSTheme {
        BackupScreenContent(
            backupState = backupState,
            onBackupClicked = {},
            onRestoreClicked = {}
        )
    }
}

class BackupContentPreviewProvider : PreviewParameterProvider<BackupResults> {
    override val values: Sequence<BackupResults> = sequenceOf(
        BackupResults.Progress(33),
        BackupResults.Success(
            operation = BackupResults.Success.Operation.BACKUP,
            notesCount = 10,
            tagsCount = 5
        ),
        BackupResults.Error(
            message = "Something went wrong"
        )
    )
}



























