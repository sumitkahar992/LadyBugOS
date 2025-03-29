package com.despicable.core.common.navigation.modifiers


import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.despicable.core.common.navigation.LocalNavAnimatedVisibilityScope
import com.despicable.core.common.navigation.LocalSharedTransitionScope
import com.despicable.core.common.navigation.NoteSharedElementKey
import com.despicable.core.common.navigation.NoteSharedElementType

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedNoteTransitionModifier(
    noteId: Long,
    type: NoteSharedElementType,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = BoundsTransform { _, _ -> DefaultSpring },
    resizeMode: ResizeMode = ScaleToBounds(ContentScale.FillWidth, Alignment.Companion.Center),
) = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: return@composed this // Fallback if scope is missing
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
        ?: return@composed this // Fallback if scope is missing

    val roundedCornerAnimation by animatedVisibilityScope.transition.animateDp(
        label = "Rounded corner",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) {
        if (it == EnterExitState.Visible) 12.dp else 4.dp
    }


    with(sharedTransitionScope) {
        val modifier = this@composed
            .skipToLookaheadSize() // Apply skipToLookaheadSize directly
            .sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = NoteSharedElementKey(noteId, type)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                clipInOverlayDuringTransition = OverlayClip(
                    RoundedCornerShape(roundedCornerAnimation)
                ),
                enter = enter,
                exit = exit,
                resizeMode = resizeMode,
                boundsTransform = boundsTransform
            )
        // Apply clipping to the composable itself if roundedCornerAnim is provided

        modifier.clip(RoundedCornerShape(roundedCornerAnimation))

    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.skipToLookaheadSizeSimplified() = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            this@composed.skipToLookaheadSize()
        }
    } else {
        this
    }
}