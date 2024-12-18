package com.despicable.core.common.navigation

/*
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@OptIn(ExperimentalSharedTransitionApi::class)
object SharedTransitionManager {
    private const val TRANSITION_DURATION = 400

    // Spring specs for different animation types
    val boundsSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 400f
    )

    val contentSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 300f
    )

    // Transition specs
    private val enterTransition = fadeIn(
        animationSpec = tween(TRANSITION_DURATION)
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        initialScale = 0.95f
    )

    private val exitTransition = fadeOut(
        animationSpec = tween(TRANSITION_DURATION)
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        targetScale = 0.95f
    )

    @Composable
    fun NoteTransitionContainer(
        noteId: Long,
        containerColor: Color,
        initialCornerRadius: Dp = 12.dp,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val sharedTransitionScope = LocalSharedTransitionScope.current
            ?: throw IllegalStateException("SharedTransitionScope not found")
        val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
            ?: throw IllegalStateException("AnimatedVisibilityScope not found")

        // Animate corner radius
        val cornerRadius by animatedVisibilityScope.transition.animateDp(
            label = "corner",
            transitionSpec = {
                spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            }
        ) { state ->
            when (state) {
                EnterExitState.Visible -> 0.dp
                else -> initialCornerRadius
            }
        }

        // Animate elevation
        val elevation by animatedVisibilityScope.transition.animateDp(
            label = "elevation",
            transitionSpec = {
                spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            }
        ) { state ->
            when (state) {
                EnterExitState.Visible -> 4.dp
                else -> 1.dp
            }
        }

        // Ensure non-negative values
        val safeCornerRadius = max(cornerRadius.value, 0f).dp
        val safeElevation = max(elevation.value, 0f).dp

        with(sharedTransitionScope) {
            Surface(
                modifier = modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = NoteSharedElementKey(noteId, NoteSharedElementType.Bounds)
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                        clipInOverlayDuringTransition = OverlayClip(
                            RoundedCornerShape(safeCornerRadius)
                        )
                    ),
                shape = RoundedCornerShape(safeCornerRadius),
                color = containerColor,
                tonalElevation = safeElevation,
                shadowElevation = safeElevation
            ) {
                Column(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = NoteSharedElementKey(noteId, NoteSharedElementType.Content)
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            enter = enterTransition,
                            exit = exitTransition
                        )
                ) {
                    content()
                }
            }
        }
    }
} */
