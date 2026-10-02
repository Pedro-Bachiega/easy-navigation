package com.pedrobneto.easy.navigation.core

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.pedrobneto.easy.navigation.core.adaptive.rememberDefaultSceneStrategies
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.transition.NavigationTransitions
import com.pedrobneto.easy.navigation.test.KoverExcludes
import kotlinx.serialization.json.Json

/**
 * Displays an existing controller and provides it to the composition.
 * The controller already owns the back stack and direction registries.
 */
@Composable
@ExperimentalMaterial3AdaptiveApi
@KoverExcludes
@Suppress("ComposableNaming")
fun Navigation(
    modifier: Modifier,
    controller: NavigationController,
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavigationRoute>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    transitions: NavigationTransitions = NavigationTransitions(),
    sceneStrategies: List<SceneStrategy<NavigationRoute>> = rememberDefaultSceneStrategies(),
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<NavigationRoute>> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
) = CompositionLocalProvider(
    LocalNavigationController provides controller,
    LocalParentNavigationController provides controller
) {
    NavDisplay(
        modifier = modifier,
        backStack = controller.backStack,
        entryProvider = controller.directionProvider,
        contentAlignment = contentAlignment,
        entryDecorators = entryDecorators,
        sceneStrategies = sceneStrategies,
        sceneDecoratorStrategies = sceneDecoratorStrategies,
        sharedTransitionScope = sharedTransitionScope,
        sizeTransform = sizeTransform,
        transitionSpec = transitions.regular.transitionSpec,
        popTransitionSpec = transitions.regular.popTransitionSpec,
        predictivePopTransitionSpec = transitions.regular.predictivePopTransitionSpec,
        onBack = controller::handleSystemBack,
    )
}

/**
 * Creates navigation from a route.
 * Initial arguments only apply to a new stack; saved state takes precedence.
 */
@Composable
@ExperimentalMaterial3AdaptiveApi
@KoverExcludes
@Suppress("ComposableNaming")
fun Navigation(
    modifier: Modifier,
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavigationRoute>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    transitions: NavigationTransitions = NavigationTransitions(),
    sceneStrategies: List<SceneStrategy<NavigationRoute>> = rememberDefaultSceneStrategies(),
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<NavigationRoute>> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
) = Navigation(
    modifier = modifier,
    controller = rememberNavigationController(
        initialRoute = initialRoute,
        directionRegistries = directionRegistries,
        json = json,
    ),
    contentAlignment = contentAlignment,
    entryDecorators = entryDecorators,
    transitions = transitions,
    sceneStrategies = sceneStrategies,
    sceneDecoratorStrategies = sceneDecoratorStrategies,
    sharedTransitionScope = sharedTransitionScope,
    sizeTransform = sizeTransform,
)

/**
 * Creates navigation from a deeplink.
 * Initial arguments only apply to a new stack; saved state takes precedence.
 */
@Composable
@ExperimentalMaterial3AdaptiveApi
@KoverExcludes
@Suppress("ComposableNaming")
fun Navigation(
    modifier: Modifier,
    initialRoute: String,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavigationRoute>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    transitions: NavigationTransitions = NavigationTransitions(),
    sceneStrategies: List<SceneStrategy<NavigationRoute>> = rememberDefaultSceneStrategies(),
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<NavigationRoute>> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
) = Navigation(
    modifier = modifier,
    controller = rememberNavigationController(
        initialRoute = initialRoute,
        directionRegistries = directionRegistries,
        json = json,
    ),
    contentAlignment = contentAlignment,
    entryDecorators = entryDecorators,
    transitions = transitions,
    sceneStrategies = sceneStrategies,
    sceneDecoratorStrategies = sceneDecoratorStrategies,
    sharedTransitionScope = sharedTransitionScope,
    sizeTransform = sizeTransform,
)

/**
 * Creates navigation from a deeplink and serializable payload.
 * Initial arguments only apply to a new stack; saved state takes precedence.
 */
@Composable
@ExperimentalMaterial3AdaptiveApi
@KoverExcludes
@Suppress("ComposableNaming")
inline fun <reified T> Navigation(
    modifier: Modifier,
    initialRoute: String,
    payload: T,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
    contentAlignment: Alignment = Alignment.TopStart,
    entryDecorators: List<NavEntryDecorator<NavigationRoute>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    transitions: NavigationTransitions = NavigationTransitions(),
    sceneStrategies: List<SceneStrategy<NavigationRoute>> = rememberDefaultSceneStrategies(),
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<NavigationRoute>> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
) = Navigation(
    modifier = modifier,
    controller = rememberNavigationController(
        initialRoute = initialRoute,
        payload = payload,
        directionRegistries = directionRegistries,
        json = json,
    ),
    contentAlignment = contentAlignment,
    entryDecorators = entryDecorators,
    transitions = transitions,
    sceneStrategies = sceneStrategies,
    sceneDecoratorStrategies = sceneDecoratorStrategies,
    sharedTransitionScope = sharedTransitionScope,
    sizeTransform = sizeTransform,
)
