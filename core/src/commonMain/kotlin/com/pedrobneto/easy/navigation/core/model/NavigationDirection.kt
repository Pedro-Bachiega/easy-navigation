package com.pedrobneto.easy.navigation.core.model

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Represents a direction in the navigation graph.
 *
 * Each direction is associated with a [NavigationRoute] and can have an optional deeplink.
 *
 * @property routeClass The class of the [NavigationRoute] associated with this direction.
 * @property deeplinks The deeplinks associated with this direction.
 * @property parentRouteClass The class of the parent [NavigationRoute] for this direction.
 * If the current route is at the root of the backstack and navigateUp or popTo(inclusive = true)
 * is called, this will be the route to navigate to. If `null`, nothing happens.
 */
abstract class NavigationDirection(
    val deeplinks: List<NavigationDeeplink>,
    val routeClass: KClass<out NavigationRoute>,
    val parentRouteClass: KClass<out NavigationRoute>? = null
) {
    /**
     * Draws the composable content for the given [route].
     *
     * @param route The [NavigationRoute] to draw.
     */
    @Composable
    abstract fun Draw(route: NavigationRoute)
}