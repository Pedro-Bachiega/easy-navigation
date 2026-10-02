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
 * Displays the current entry of an existing [NavigationController] using [NavDisplay].
 * Provides the controller to the composition through [LocalNavigationController].
 * The controller already owns the back stack and direction registries.
 *
 * @param modifier The modifier applied to the navigation container.
 * @param controller The existing navigation controller whose back stack and destinations are displayed.
 * @param contentAlignment The alignment of the content within the navigation container.
 * @param entryDecorators The decorators applied to each navigation entry, including saveable state support by default.
 * @param transitions The regular scene transitions for forward, pop, and predictive pop navigation. Modal destinations use their own transitions.
 * @param sceneStrategies The strategies used to select and display scenes, including the default adaptive strategies.
 * @param sceneDecoratorStrategies The decorators applied to each scene.
 * @param sharedTransitionScope The optional shared transition scope used for transitions between scenes.
 * @param sizeTransform The optional transform applied when the size of the navigation content changes.
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
 * Provides the remembered controller through [LocalNavigationController] and displays its current entry.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param modifier The modifier applied to the navigation container.
 * @param initialRoute The initial [NavigationRoute] placed on a new back stack.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @param contentAlignment The alignment of the content within the navigation container.
 * @param entryDecorators The decorators applied to each navigation entry, including saveable state support by default.
 * @param transitions The regular scene transitions for forward, pop, and predictive pop navigation. Modal destinations use their own transitions.
 * @param sceneStrategies The strategies used to select and display scenes, including the default adaptive strategies.
 * @param sceneDecoratorStrategies The decorators applied to each scene.
 * @param sharedTransitionScope The optional shared transition scope used for transitions between scenes.
 * @param sizeTransform The optional transform applied when the size of the navigation content changes.
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
 * Provides the remembered controller through [LocalNavigationController] and displays its current entry.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param modifier The modifier applied to the navigation container.
 * @param initialRoute The initial deeplink resolved against directionRegistries when creating a new back stack. Resolution errors propagate to the caller.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @param contentAlignment The alignment of the content within the navigation container.
 * @param entryDecorators The decorators applied to each navigation entry, including saveable state support by default.
 * @param transitions The regular scene transitions for forward, pop, and predictive pop navigation. Modal destinations use their own transitions.
 * @param sceneStrategies The strategies used to select and display scenes, including the default adaptive strategies.
 * @param sceneDecoratorStrategies The decorators applied to each scene.
 * @param sharedTransitionScope The optional shared transition scope used for transitions between scenes.
 * @param sizeTransform The optional transform applied when the size of the navigation content changes.
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
 * Provides the remembered controller through [LocalNavigationController] and displays its current entry.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param modifier The modifier applied to the navigation container.
 * @param initialRoute The initial deeplink resolved against directionRegistries when creating a new back stack. Resolution errors propagate to the caller.
 * @param payload The serializable payload for the initial deeplink. It must encode to a JSON object; its arguments override query and path arguments.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @param contentAlignment The alignment of the content within the navigation container.
 * @param entryDecorators The decorators applied to each navigation entry, including saveable state support by default.
 * @param transitions The regular scene transitions for forward, pop, and predictive pop navigation. Modal destinations use their own transitions.
 * @param sceneStrategies The strategies used to select and display scenes, including the default adaptive strategies.
 * @param sceneDecoratorStrategies The decorators applied to each scene.
 * @param sharedTransitionScope The optional shared transition scope used for transitions between scenes.
 * @param sizeTransform The optional transform applied when the size of the navigation content changes.
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
