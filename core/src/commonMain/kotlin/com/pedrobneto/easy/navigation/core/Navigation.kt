package com.pedrobneto.easy.navigation.core

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/**
 * A composable that provides a navigation controller and displays the current navigation entry.
 *
 * This composable is the root of the navigation system. It creates a [NavigationController]
 * and provides it to the composition via [LocalNavigationController]. It also displays the
 * current navigation entry using [NavDisplay].
 *
 * @param modifier The modifier to be applied to the navigation container.
 * @param initialRoute The initial route to be displayed.
 * @param directionRegistries The list of direction registries to be used for navigation.
 * @param contentAlignment The alignment of the content within the navigation container.
 * @param entryDecorators A list of decorators to be applied to each navigation entry.
 * @param sceneStrategy The strategy to be used for displaying scenes.
 * @param sizeTransform The transform to be applied to the size of the content.
 * @param transitionSpec The transition spec to be used for transitions between scenes.
 * @param popTransitionSpec The transition spec to be used for pop transitions between scenes.
 * @param predictivePopTransitionSpec The transition spec to be used for predictive pop transitions between scenes.
 */
@Composable
fun Navigation(
    modifier: Modifier,
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavigationRoute>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategy: SceneStrategy<NavigationRoute> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<NavigationRoute>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationRoute>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationRoute>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = defaultPredictivePopTransitionSpec(),
) = CompositionLocalProvider(
    LocalNavigationController provides NavigationController(
        initialRoute = initialRoute,
        directionRegistries = directionRegistries,
    )
) {
    val navigation = LocalNavigationController.current
    NavDisplay(
        modifier = modifier,
        backStack = navigation.backStack,
        entryProvider = navigation.directionProvider,
        contentAlignment = contentAlignment,
        entryDecorators = entryDecorators,
        sceneStrategy = sceneStrategy,
        sizeTransform = sizeTransform,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
    )
}
