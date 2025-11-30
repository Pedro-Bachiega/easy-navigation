package com.pedrobneto.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import br.com.arch.toolkit.lumber.Lumber
import com.pedrobneto.navigation.core.launch.LaunchStrategy
import com.pedrobneto.navigation.core.model.DirectionRegistry
import com.pedrobneto.navigation.core.model.NavigationDeeplink
import com.pedrobneto.navigation.core.model.NavigationDirection
import com.pedrobneto.navigation.core.model.NavigationRoute
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A CompositionLocal that provides access to the [NavigationController] instance.
 * This should be used to access the navigation controller from within a composable.
 */
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
class NavigationController private constructor(
    val backStack: SnapshotStateList<NavigationRoute>,
    private val directionRegistryList: List<DirectionRegistry>,
    private val json: Json
) {
    private val directions: List<NavigationDirection> =
        directionRegistryList.flatMap(DirectionRegistry::directions)

    /**
     * Provides a [NavEntry] for a given [NavigationRoute], allowing the navigation framework
     * to render the correct composable for each route.
     */
    internal val directionProvider: (NavigationRoute) -> NavEntry<NavigationRoute> =
        entryProvider {
            directions.map { direction ->
                addEntryProvider(clazz = direction.routeClass, content = direction::Draw)
            }
        }

    /**
     * Pops the top-most destination from the back stack.
     *
     * @throws IllegalStateException if `at the root of the back stack.
     */
    fun navigateUp() {
        if (backStack.size == 1) {
            // TODO Close navigation when root popped
            throw IllegalStateException("Cannot pop root destination")
        }

        backStack.removeAt(backStack.lastIndex)
    }

    /**
     * Pops the top-most destination from the back stack.
     *
     * @throws IllegalStateException if `at the root of the back stack.
     */
    fun safeNavigateUp(): Boolean = runCatching { navigateUp() }.isSuccess

    /**
     * Navigates to a given [NavigationRoute].
     *
     * The behavior of this navigation action is determined by the provided [LaunchStrategy].
     *
     * @param route The destination [NavigationRoute] to navigate to.
     * @param strategy The [LaunchStrategy] to apply to this navigation. Defaults to pushing a new
     * destination on the stack.
     */
    fun navigateTo(route: NavigationRoute, strategy: LaunchStrategy = LaunchStrategy.NewTask()) =
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
    @Throws(IllegalArgumentException::class)
    fun navigateTo(deeplink: String, strategy: LaunchStrategy = LaunchStrategy.NewTask()) =
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
    fun safeNavigateTo(
        deeplink: String,
        strategy: LaunchStrategy = LaunchStrategy.NewTask()
    ): Boolean = runCatching { navigateTo(deeplink, strategy) }.isSuccess

    /**
     * Pops the back stack up to a given destination route.
     *
     * This will remove all destinations from the top of the stack down to the specified [route].
     *
     * @param route The destination [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the destination itself will also be popped from the stack.
     * @throws IllegalStateException if `inclusive` is true and the target `direction` is the root of the back stack.
     */
    @Throws(IllegalStateException::class)
    fun popUpTo(route: NavigationRoute, inclusive: Boolean = false) {
        if (route !in backStack) return
        popUpTo(backStack.indexOfLast { it == route }, inclusive)
    }

    /**
     * Pops the back stack up to a given destination route.
     *
     * This will remove all destinations from the top of the stack down to the specified [routeClass].
     *
     * @param routeClass The destination [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the destination itself will also be popped from the stack.
     * @throws IllegalStateException if `inclusive` is true and the target `direction` is the root of the back stack.
     */
    @Throws(IllegalStateException::class)
    fun popUpTo(routeClass: KClass<out NavigationRoute>, inclusive: Boolean = false) {
        if (backStack.none { it::class == routeClass }) return
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
    fun safePopUpTo(direction: NavigationRoute, inclusive: Boolean = false): Boolean =
        runCatching { popUpTo(direction, inclusive) }.isSuccess

    @Throws(IllegalStateException::class)
    private fun popUpTo(index: Int, inclusive: Boolean = false) {
        if (index == 0 && inclusive) {
            // TODO Close navigation when root popped
            Lumber.tag("NavigationController").error("Cannot pop root destination.")
            throw IllegalStateException("Cannot pop root destination.")
        }

        val startIndex = if (inclusive) index else index + 1
        backStack.removeRange(startIndex, backStack.size)
    }

    companion object {
        /**
         * Creates and remembers a [NavigationController] instance.
         *
         * @param initialRoute The initial route to be displayed when the navigation is first set up.
         * @param directionRegistries A list of [DirectionRegistry] instances containing all possible navigation directions.
         * @param backStack An optional [SnapshotStateList] to be used as the back stack. If not provided,
         * a new one will be created with the [initialRoute].
         * @param json The [Json] instance used for deserializing route arguments.
         * @return A remembered [NavigationController] instance.
         */
        @Composable
        operator fun invoke(
            initialRoute: NavigationRoute,
            directionRegistries: List<DirectionRegistry>,
            backStack: SnapshotStateList<NavigationRoute> = remember {
                mutableStateListOf(initialRoute)
            },
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
