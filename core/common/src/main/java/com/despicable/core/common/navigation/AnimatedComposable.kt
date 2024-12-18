package com.despicable.core.common.navigation
/*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable


private const val DEFAULT_FADE_DURATION = 300
private const val DEFAULT_SCALE_DURATION = 400
private const val DEFAULT_SLIDE_DURATION = 400
private const val DEFAULT_INITIAL_SCALE = 0.9f

// Common composable function for transitions
fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: () -> EnterTransition = ::defaultScreenEnterAnimation,
    exitTransition: () -> ExitTransition = ::defaultScreenExitAnimation,
    popEnterTransition: () -> EnterTransition = ::defaultScreenEnterAnimation,
    popExitTransition: () -> ExitTransition = ::defaultScreenExitAnimation,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = { enterTransition() },
    exitTransition = { exitTransition() },
    popEnterTransition = { popEnterTransition() },
    popExitTransition = { popExitTransition() },
    content = content
)

// Optimized animation functions
fun fadeScaleEnterAnimation(): EnterTransition = fadeIn(tween(DEFAULT_FADE_DURATION)) +
        scaleIn(initialScale = DEFAULT_INITIAL_SCALE, animationSpec = tween(DEFAULT_SCALE_DURATION))

fun fadeScaleExitAnimation(): ExitTransition = fadeOut(tween(DEFAULT_FADE_DURATION)) +
        scaleOut(targetScale = DEFAULT_INITIAL_SCALE, animationSpec = tween(DEFAULT_SCALE_DURATION))

fun slideInAnimation(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(DEFAULT_SLIDE_DURATION)
)

fun slideOutAnimation(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(DEFAULT_SLIDE_DURATION)
)

// Default transition functions for easy reusability
fun defaultScreenEnterAnimation(): EnterTransition = fadeScaleEnterAnimation()
fun defaultScreenExitAnimation(): ExitTransition = fadeScaleExitAnimation()
fun slideScreenEnterAnimation(): EnterTransition = slideInAnimation()
fun slideScreenExitAnimation(): ExitTransition = slideOutAnimation()




fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = { defaultScreenEnterAnimation() },
    exitTransition = { defaultScreenExitAnimation() },
    popEnterTransition = { defaultScreenEnterAnimation() },
    popExitTransition = { defaultScreenExitAnimation() },
    content = content
)

fun NavGraphBuilder.slideInComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = { slideScreenEnterAnimation() },
    exitTransition = { defaultScreenExitAnimation() },
    popEnterTransition = { defaultScreenEnterAnimation() },
    popExitTransition = { slideScreenExitAnimation() },
    content = content
)


private const val DEFAULT_FADE_DURATION = 300
private const val DEFAULT_SCALE_DURATION = 400
private const val DEFAULT_SLIDE_DURATION = 400
private const val DEFAULT_INITIAL_SCALE = 0.9f

fun getNoteEnterAnimation(): EnterTransition {
    return fadeIn(animationSpec = tween(DEFAULT_FADE_DURATION)) + scaleIn(
        initialScale = DEFAULT_INITIAL_SCALE,
        animationSpec = tween(DEFAULT_SCALE_DURATION)
    )
}

fun getNoteExitAnimation(slideDirection: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { slideDirection * it },
        animationSpec = tween(durationMillis = DEFAULT_SLIDE_DURATION)
    ) + fadeOut(animationSpec = tween(durationMillis = DEFAULT_FADE_DURATION))
}

fun defaultScreenEnterAnimation(): EnterTransition {
    return getNoteEnterAnimation()
}

fun defaultScreenExitAnimation(): ExitTransition {
    return fadeOut(animationSpec = tween(DEFAULT_FADE_DURATION)) +
            scaleOut(
                targetScale = DEFAULT_INITIAL_SCALE,
                animationSpec = tween(DEFAULT_SCALE_DURATION)
            )
}

fun slideScreenEnterAnimation(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DEFAULT_SLIDE_DURATION)
    )
}

fun slideScreenExitAnimation(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DEFAULT_SLIDE_DURATION)
    )
}

*/
