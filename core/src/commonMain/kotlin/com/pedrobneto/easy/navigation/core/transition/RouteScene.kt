package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import com.pedrobneto.easy.navigation.core.NavigationController
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

internal const val TRANSITION_ROUTE_METADATA_KEY = "com.pedrobneto.easy.navigation.transition.route"

internal val NavEntry<NavigationRoute>.transitionRoute: NavigationRoute
    get() = requireNotNull(metadata[TRANSITION_ROUTE_METADATA_KEY] as? NavigationRoute) {
        "Navigation entries must preserve the route metadata provided by NavigationController."
    }

/**
 * Captures the logical stack when the scene is calculated, including projected predictive-back
 * scenes. It deliberately does not infer stack order from Scene.entries.
 */
internal data class RouteScene(
    val scene: Scene<NavigationRoute>,
    val stack: List<NavigationRoute>,
    val change: NavigationTransitionChange?,
) : Scene<NavigationRoute> by scene {
    // NavDisplay combines scene class and key. Preserve the original class as part of our key
    // so two different scene types with the same key remain distinct after wrapping.
    override val key: Any = scene::class to scene.key
    val route: NavigationRoute get() = stack.last()
}

internal val Scene<NavigationRoute>.originalScene: Scene<NavigationRoute>
    get() = (this as? RouteScene)?.scene ?: this

internal class RouteSceneStrategies(
    val strategies: List<SceneStrategy<NavigationRoute>>,
    val decorators: List<SceneDecoratorStrategy<NavigationRoute>>,
)

@Composable
internal fun rememberRouteSceneStrategies(
    controller: NavigationController,
    strategies: List<SceneStrategy<NavigationRoute>>,
    decorators: List<SceneDecoratorStrategy<NavigationRoute>>,
): RouteSceneStrategies {
    val change = controller.transitionChange
    return remember(controller, strategies.toList(), decorators.toList(), change) {
        routeSceneStrategies(strategies, decorators, change)
    }
}

internal fun routeSceneStrategies(
    strategies: List<SceneStrategy<NavigationRoute>>,
    decorators: List<SceneDecoratorStrategy<NavigationRoute>>,
    change: NavigationTransitionChange?,
): RouteSceneStrategies {
    val fallback = SinglePaneSceneStrategy<NavigationRoute>()
    val capturingStrategy = SceneStrategy<NavigationRoute> { entries ->
        val scene = strategies.firstNotNullOfOrNull { strategy ->
            with(strategy) { calculateScene(entries) }
        } ?: with(fallback) { calculateScene(entries) }

        // Overlays retain their own lifecycle, type, and animations.
        if (scene is OverlayScene<NavigationRoute>) {
            scene
        } else {
            RouteScene(scene, entries.map { it.transitionRoute }, change)
        }
    }
    val capturingDecorator = SceneDecoratorStrategy<NavigationRoute> { captured ->
        val snapshot = captured as RouteScene
        val decorated = decorators.fold(snapshot.scene) { scene, decorator ->
            with(decorator) { decorateScene(scene) }
        }
        // Consumer decorators see the original scene, and callbacks see the final decorated scene.
        if (decorated is OverlayScene<NavigationRoute>) decorated else snapshot.copy(scene = decorated)
    }
    return RouteSceneStrategies(listOf(capturingStrategy), listOf(capturingDecorator))
}
