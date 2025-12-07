@file:OptIn(InternalSerializationApi::class)

package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import br.com.arch.toolkit.lumber.Lumber
import com.pedrobneto.easy.navigation.core.annotation.SafeNavigationApi
import com.pedrobneto.easy.navigation.core.annotation.UnsafeNavigationApi
import com.pedrobneto.easy.navigation.core.extension.rememberNavBackStack
import com.pedrobneto.easy.navigation.core.extension.removeRange
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.test.KoverExcludes
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * A CompositionLocal that provides access to the [NavigationController] instance.
 * This should be used to access the navigation controller from within a composable.
 */
@KoverExcludes
val LocalNavigationController: ProvidableCompositionLocal<NavigationController> =
    staticCompositionLocalOf { error("Navigation not initialized. Make sure you have a Navigation composable in your hierarchy.") }

/**
 * A controller that manages the navigation state and back stack of the application.
 *
 * It provides functionalities to navigate between different destinations using routes or deeplinks,
 * handle the back stack, and integrate with the underlying navigation framework.
 *
 * This controller should be created and provided at the root of your navigation graph using the
 * [Navigation] composable.
 *
 * @property backStack A reactive list of [NavigationRoute]s representing the current navigation back stack.
 * @param directionRegistryList A list of [DirectionRegistry] instances that contain all possible navigation directions.
 * @param json The [Json] instance used for deserializing route arguments.
 */
class NavigationController internal constructor(
    internal val backStack: NavBackStack<NavigationRoute>,
    private val directionRegistryList: List<DirectionRegistry>,
    private val json: Json,
) {
    private val directions: List<NavigationDirection> =
        directionRegistryList.flatMap(DirectionRegistry::directions)

    internal val currentDirection: NavigationDirection
        get() = directions.find { it.routeClass == currentRoute::class }
            ?: error("No direction found for route $currentRoute")

    /**
     * Provides a [NavEntry] for a given [NavigationRoute], allowing the navigation framework
     * to render the correct composable for each route.
     */
    internal val directionProvider: (NavigationRoute) -> NavEntry<NavigationRoute> = entryProvider {
        directions.map { direction ->
            addEntryProvider(
                clazz = direction.routeClass,
                metadata = direction.metadata,
                content = direction::Draw
            )
        }
    }

    /**
     * The current route in the navigation back stack.
     *
     * This is the last element in the [backStack] list.
     */
    val currentRoute: NavigationRoute get() = backStack.last()

    /**
     * The current index in the navigation back stack.
     *
     * This is the index of the last element in the [backStack] list.
     */
    val currentIndex: Int get() = backStack.lastIndex

    /**
     * Navigates to a given [NavigationRoute].
     *
     * The behavior of this navigation action is determined by the provided [LaunchStrategy].
     *
     * @param route The destination [NavigationRoute] to navigate to.
     * @param strategy The [LaunchStrategy] to apply to this navigation. Defaults to pushing a new
     * destination on the stack.
     */
    fun navigateTo(route: NavigationRoute, strategy: LaunchStrategy = LaunchStrategy.Default) =
        strategy.handleNavigation(route = route, controller = this)


    /**
     * Navigates to a destination via a deeplink URI.
     *
     * This method parses the deeplink to find a matching [NavigationDirection] and constructs
     * the [NavigationRoute] with its arguments from the deeplink's query parameters.
     *
     * @param deeplink The deeplink URI to navigate to.
     * @param strategy The [LaunchStrategy] to apply to this navigation action.
     * @throws IllegalArgumentException if no direction is found for the deeplink or if the
     * deeplink is malformed for the target route.
     */
    @UnsafeNavigationApi
    @Throws(IllegalArgumentException::class)
    fun navigateTo(deeplink: String, strategy: LaunchStrategy = LaunchStrategy.Default) =
        navigateTo(
            route = NavigationDeeplink(deeplink).resolve(json, directions),
            strategy = strategy
        )

    /**
     * Navigates to a destination via a deeplink URI.
     *
     * This method parses the deeplink to find a matching [NavigationDirection] and constructs
     * the [NavigationRoute] with its arguments from the deeplink's query parameters.
     *
     * @param deeplink The deeplink URI to navigate to.
     * @param strategy The [LaunchStrategy] to apply to this navigation action.
     * @return `true` if the navigation was successful, `false` otherwise.
     */
    @SafeNavigationApi
    fun safeNavigateTo(
        deeplink: String,
        strategy: LaunchStrategy = LaunchStrategy.Default
    ): Boolean = runCatching { navigateTo(deeplink, strategy) }.isSuccess

    /**
     * Pops the top-most destination from the back stack.
     *
     * If the back stack has 1 route and the route has a parent, it will navigate to the parent route.
     *
     * @throws [IllegalArgumentException] if the parent route does not have a constructor with 0 parameters or a constructor where all parameters have default values.
     * @throws [IllegalStateException] if at the root of the back stack and there is no parent route.
     */
    @UnsafeNavigationApi
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun navigateUp() = popUpTo(targetRouteIndex = backStack.lastIndex - 1)

    /**
     * Pops the top-most destination from the back stack.
     *
     * @return `true` if the navigation was successful, `false` otherwise.
     */
    @SafeNavigationApi
    fun safeNavigateUp(): Boolean = runCatching { navigateUp() }.isSuccess

    /**
     * Pops the back stack up to a given `route` [NavigationRoute].
     *
     * This will remove all routes from the top of the stack down to the specified [route].
     *
     * @param route The `route` [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the `route` itself will also be popped from the stack.
     * @throws IllegalArgumentException if the target `route` is not found in the back stack.
     * @throws IllegalStateException if `inclusive` is true, the target `route` is the root of the back stack
     * and there is no parent route provided for that `route`. If a parent `route` is provided for that `route`,
     * it will be used as a destination to navigate to instead of throwing an exception.
     */
    @UnsafeNavigationApi
    @Throws(IllegalStateException::class)
    fun popUpTo(route: NavigationRoute, inclusive: Boolean = false) {
        if (route !in backStack) {
            Lumber.tag("NavigationController").error("No route found in back stack for $route")
            throw IllegalArgumentException("No route found in back stack for $route")
        }
        popUpTo(backStack.indexOfLast { it == route }, inclusive)
    }

    /**
     * Pops the back stack up to a given `route` [NavigationRoute].
     *
     * This will remove all routes from the top of the stack down to the specified [routeClass].
     *
     * @param routeClass The `route` [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the `route` itself will also be popped from the stack.
     * @throws IllegalArgumentException if the target `route` is not found in the back stack.
     * @throws IllegalStateException if `inclusive` is true, the target `route` is the root of the back stack
     * and there is no parent route provided for that `route`. If a parent `route` is provided for that `route`,
     * it will be used as a destination to navigate to instead of throwing an exception.
     */
    @UnsafeNavigationApi
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun popUpTo(routeClass: KClass<out NavigationRoute>, inclusive: Boolean = false) {
        if (backStack.none { it::class == routeClass }) {
            val message = "No route found in back stack for $routeClass"
            Lumber.tag("NavigationController").error(message)
            throw IllegalArgumentException(message)
        }
        popUpTo(backStack.indexOfLast { it::class == routeClass }, inclusive)
    }

    /**
     * Pops the back stack up to a given destination route.
     *
     * This will remove all destinations from the top of the stack down to the specified [direction].
     *
     * @param direction The destination [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the destination itself will also be popped from the stack.
     * @return `true` if the pop operation was successful, `false` otherwise.
     */
    @SafeNavigationApi
    fun safePopUpTo(direction: NavigationRoute, inclusive: Boolean = false): Boolean =
        runCatching { popUpTo(direction, inclusive) }.isSuccess

    @Throws(IllegalStateException::class)
    private fun popUpTo(targetRouteIndex: Int, inclusive: Boolean = false) {
        val parentDeeplink = currentDirection.parentDeeplink
        val parentRouteClass = currentDirection.parentRouteClass
        val hasParent = parentDeeplink != null || parentRouteClass != null

        val actualFirstRemovedIndex = if (inclusive) targetRouteIndex else targetRouteIndex + 1
        val isPoppingRoot = actualFirstRemovedIndex <= 0
        val shouldClose = isPoppingRoot && !hasParent
        val shouldTryNavigatingToParent = isPoppingRoot && hasParent

        when {
            shouldClose -> {
                // TODO Close navigation when root popped
                val message = "Cannot pop root destination."
                Lumber.tag("NavigationController").error(message)
                throw IllegalStateException(message)
            }

            shouldTryNavigatingToParent -> {
                runCatching {
                    val route = parentDeeplink?.resolve(json, directions)
                        ?: parentRouteClass?.serializer()?.let {
                            json.decodeFromString(it, "{}")
                        }

                    when {
                        route == null && parentRouteClass != null -> {
                            error(
                                "Parent route should either have a constructor with" +
                                        " 0 parameters or all parameters should have default values."
                            )
                        }

                        route == null && parentDeeplink != null -> {
                            error("Could not resolve deeplink for parent $parentDeeplink")
                        }

                        route != null -> backStack[currentIndex] = route
                    }
                }.getOrElse { exception ->
                    val message = "Could not decode route for parent"
                    Lumber.tag("NavigationController").error(exception, message)
                    throw IllegalArgumentException(message, exception)
                }
            }

            else -> {
                val startIndex = if (inclusive) targetRouteIndex else targetRouteIndex + 1
                backStack.removeRange(startIndex, backStack.size)
            }
        }
    }

    companion object {
        /**
         * Creates and remembers a [NavigationController] instance.
         *
         * @param initialRoute The initial route to be displayed when the navigation is first set up.
         * @param directionRegistries A list of [DirectionRegistry] instances containing all possible navigation directions.
         * @param backStack An optional [NavBackStack] to be used as the back stack. If not provided,
         * a new one will be created with the [initialRoute].
         * @param json The [Json] instance used for deserializing route arguments.
         * @return A remembered [NavigationController] instance.
         */
        @Composable
        @KoverExcludes
        operator fun invoke(
            initialRoute: NavigationRoute,
            directionRegistries: List<DirectionRegistry>,
            backStack: NavBackStack<NavigationRoute> = rememberNavBackStack(
                initialRoute,
                directionRegistries
            ),
            json: Json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                prettyPrint = true
            }
        ) = NavigationController(
            backStack = backStack,
            directionRegistryList = directionRegistries,
            json = json
        )
    }
}
