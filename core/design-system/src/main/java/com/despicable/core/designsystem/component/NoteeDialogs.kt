package com.despicable.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun NoteeDialog(
    enabled: Boolean = false,
    title: String,
    description: String,
    icon: ImageVector? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseNoteeDialog(
        enabled = enabled,
        title = title,
        icon = icon,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NoteeDialog(
    enabled: Boolean = false,
    title: String,
    description: @Composable () -> Unit,
    icon: ImageVector? = null,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseNoteeDialog(
        enabled = enabled,
        title = title,
        icon = icon,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        content = description
    )
}

@Composable
private fun BaseNoteeDialog(
    enabled: Boolean,
    title: String,
    icon: ImageVector?,
    confirmText: String?,
    dismissText: String?,
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        icon = icon?.let { iconVector ->
            {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        text = content,
        confirmButton = {
            confirmText?.let { text ->
                onConfirm?.let { confirm ->
                    Button(onClick = confirm) {
                        Text(text = text)
                    }
                }
            }
        },
        dismissButton = {
            dismissText?.let { text ->
                TextButton(onClick = onDismiss) {
                    Text(text = text)
                }
            }
        },
        modifier = modifier
    )
}
