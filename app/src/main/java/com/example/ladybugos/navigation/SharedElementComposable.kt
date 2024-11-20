package com.example.ladybugos.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType


/*
@ExperimentalSharedTransitionApi
fun NavGraphBuilder.sharedElementComposable(
    sharedTransitionScope: SharedTransitionScope,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = { materialSharedAxisZIn(forward = true) },
    exitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = { materialSharedAxisZOut(forward = true) },
    popEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = { materialSharedAxisZIn(forward = false) },
    popExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = { materialSharedAxisZOut(forward = false) },
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(route, arguments, deepLinks, enterTransition, exitTransition, popEnterTransition, popExitTransition) {
        CompositionLocalProvider(
            LocalSharedElementScopes provides SharedElementScopes(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        ) {
            content(it)
        }
    }
}


@ExperimentalSharedTransitionApi
inline fun <reified T : Any> NavGraphBuilder.sharedElementComposable(
    sharedTransitionScope: SharedTransitionScope,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = { materialSharedAxisZIn(forward = true) },
    noinline exitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = { materialSharedAxisZOut(forward = true) },
    noinline popEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = { materialSharedAxisZIn(forward = false) },
    noinline popExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = { materialSharedAxisZOut(forward = false) },
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(typeMap, deepLinks, enterTransition, exitTransition, popEnterTransition, popExitTransition) {
        CompositionLocalProvider(
            LocalSharedElementScopes provides SharedElementScopes(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        ) {
            content(it)
        }
    }
}

@ExperimentalSharedTransitionApi
val LocalSharedElementScopes = compositionLocalOf { SharedElementScopes() }

@ExperimentalSharedTransitionApi
data class SharedElementScopes (
    val sharedTransitionScope: SharedTransitionScope? = null,
    val animatedVisibilityScope: AnimatedVisibilityScope? = null
)

*/

inline fun <reified T : Screen> NavGraphBuilder.sharedElementComposable(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        materialSharedAxisZIn(
            forward = true
        )
    },
    noinline exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        materialSharedAxisZOut(
            forward = true
        )
    },
    noinline popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
        materialSharedAxisZIn(
            forward = false
        )
    },
    noinline popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
        materialSharedAxisZOut(
            forward = false
        )
    },
    crossinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) {
    composable<T>(
        typeMap,
        deepLinks,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition
    ) {
        CompositionLocalProvider(
            LocalNavAnimatedVisibilityScope provides this@composable,
        ) {
            content(it)
        }
    }
}


fun materialSharedAxisZIn(
    forward: Boolean,
    durationMillis: Int = 300
): EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.forIncoming,
        easing = LinearOutSlowInEasing
    )
) + scaleIn(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    initialScale = if (forward) 0.8f else 1.1f
)

fun materialSharedAxisZOut(
    forward: Boolean,
    durationMillis: Int = 300
): ExitTransition = fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.forOutgoing,
        easing = FastOutLinearInEasing
    )
) + scaleOut(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    targetScale = if (forward) 1.1f else 0.8f
)

private val Int.forOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

private val Int.forIncoming: Int
    get() = this - this.forOutgoing

private const val ProgressThreshold = 0.35f


val LinearOutSlowInEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
val FastOutLinearInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
val FastOutSlowInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)


