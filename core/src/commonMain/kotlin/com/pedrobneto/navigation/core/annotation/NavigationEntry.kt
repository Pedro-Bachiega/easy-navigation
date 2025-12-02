package com.pedrobneto.navigation.core.annotation

import com.pedrobneto.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

/**
 * Marks a composable function as a navigation entry.
 *
 * This annotation is used by the navigation processor to generate navigation code.
 *
 * @property deeplinks An array of deeplink URIs that lead to this navigation entry.
 * @property route The route associated with this navigation entry.
 * @property parentRoute The parent route associated with this navigation entry. This will be used
 * to navigate to when the current route is popped and the back stack would otherwise be cleared.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class NavigationEntry(
    val deeplinks: Array<String> = [],
    val route: KClass<out NavigationRoute>,
    val parentRoute: KClass<out NavigationRoute> = NavigationRoute::class
)
