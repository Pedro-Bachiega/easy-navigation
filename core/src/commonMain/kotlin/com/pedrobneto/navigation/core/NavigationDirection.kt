package com.pedrobneto.navigation.core

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Represents a direction in the navigation graph.
 *
 * Each direction is associated with a [NavigationRoute] and can have an optional deeplink.
 *
 * @property routeClass The class of the [NavigationRoute] associated with this direction.
 * @property deeplinks The deeplinks associated with this direction.
 */
abstract class NavigationDirection(
    val routeClass: KClass<out NavigationRoute>,
    val deeplinks: List<String>
) {
    /**
     * Draws the composable content for the given [route].
     *
     * @param route The [NavigationRoute] to draw.
     */
    @Composable
    abstract fun Draw(route: NavigationRoute)
}
