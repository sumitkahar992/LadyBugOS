package com.despicable.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime


@Composable
fun ReminderInfo(
    modifier: Modifier = Modifier,
    reminderDate: Instant,
    isDone: Boolean,
    onClick: () -> Unit = {},
    isClickable: Boolean = false,
    noteColor: Int = 0
) {

    val formattedDate = remember(reminderDate) {
        formatReminderDate(reminderDate)
    }

    // Calculate colors based on theme and noteColor
    val colors = rememberTagColors(noteColor)


    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Haptic feedback
    val haptic = LocalHapticFeedback.current

    // Animation for flash effect
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) colors.surfaceColor.copy(alpha = 0.5f)
        else colors.surfaceColor,
        animationSpec = tween(
            durationMillis = if (isPressed) 50 else 200,
            easing = FastOutSlowInEasing
        ),
        label = "surface color animation"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = animatedColor,
            contentColor = colors.onSurfaceColor,
            shape = RoundedCornerShape(6.dp),
            modifier = modifier
                .height(27.dp)
                .then(
                    if (isClickable) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                            }
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(6.dp)
                    .wrapContentWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatReminderDate(reminderDate: Instant): String {
    val now = Clock.System.now()
    val reminderDateTime = reminderDate.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

    return when {
        // Same day
        reminderDateTime.date == nowDateTime.date ->
            "Today, ${formatTime(reminderDateTime.time)}"

        // Next day
        reminderDateTime.date == nowDateTime.date.plus(1, DateTimeUnit.DAY) ->
            "Tomorrow, ${formatTime(reminderDateTime.time)}"

        // Same year - don't show year
        reminderDateTime.year == nowDateTime.year ->
            "${formatDate(reminderDateTime.date)}, ${formatTime(reminderDateTime.time)}"

        // Different year - show year
        else ->
            "${formatDate(reminderDateTime.date)}, ${reminderDateTime.year}, ${
                formatTime(
                    reminderDateTime.time
                )
            }"
    }
}
