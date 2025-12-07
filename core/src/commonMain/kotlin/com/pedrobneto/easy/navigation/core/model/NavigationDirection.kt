package com.pedrobneto.easy.navigation.core.model

import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.adaptive.PaneStrategy
import kotlin.reflect.KClass

/**
 * Represents a direction in the navigation graph, defining a destination that can be navigated to.
 *
 * Each direction is associated with a [NavigationRoute] and is responsible for defining its own
 * content, deeplinks, and navigation behavior like up navigation and panel display strategy.
 *
 * @property deeplinks A list of [NavigationDeeplink]s associated with this direction, allowing for
 * navigation via URI.
 * @property routeClass The [KClass] of the [NavigationRoute] that this direction represents. This is
 * used for type-safe navigation and identifying the route.
 * @property parentDeeplink An optional [NavigationDeeplink] for the parent route. If provided, this
 * is used to construct the parent route when performing an "up" navigation from the root of a task.
 * @property parentRouteClass The optional [KClass] of a parent [NavigationRoute]. This defines the
 * logical "up" navigation target from this direction. If a user is at a screen for this direction
 * and it's the first screen in the backstack, navigating "up" will lead to this parent route.
 * @property paneStrategy The [PaneStrategy] that dictates how the UI for this direction should be
 * displayed, particularly in adaptive layouts (e.g., on tablets or foldable devices). Defaults to
 * [PaneStrategy.Adaptive].
 */
abstract class NavigationDirection(
    val deeplinks: List<NavigationDeeplink> = emptyList(),
    val routeClass: KClass<out NavigationRoute>,
    val parentDeeplink: NavigationDeeplink? = null,
    val parentRouteClass: KClass<out NavigationRoute>? = null,
    val paneStrategy: PaneStrategy = PaneStrategy.Adaptive()
) {
    internal val metadata: Map<String, Any> = mapOf(
        METADATA_ROUTE_KEY to routeClass.qualifiedName.orEmpty(),
        METADATA_STRATEGY_KEY to paneStrategy,
    )

    /**
     * Draws the composable content for the given [route].
     *
     * @param route The [NavigationRoute] to draw.
     */
    @Composable
    abstract fun Draw(route: NavigationRoute)

    internal companion object {
        const val METADATA_ROUTE_KEY = "route"
        const val METADATA_STRATEGY_KEY = "strategy"
    }
}