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
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
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
 */
class NavigationController private constructor(
    val backStack: SnapshotStateList<NavigationRoute>,
    private val directionRegistryList: List<DirectionRegistry>,
) {
    private val directions: List<NavigationDirection> =
        directionRegistryList.flatMap(DirectionRegistry::directions)

    private val String.normalized: String
        get() = when {
            startsWith("/") -> "nav:/$this"
            matches(".*://.*".toRegex()) -> this
            else -> error("Malformed deeplink '$this'. Must follow uri pattern.")
        }.withoutScheme.withoutQueryParams

    private val String.withoutScheme: String get() = split("://").last()
    private val String.withoutQueryParams: String get() = split("?").first()

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
     * @return `true` if a destination was popped, `false` if the back stack was empty.
     */
    fun navigateUp() = backStack.removeLastOrNull() != null

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
    fun navigateTo(deeplink: String, strategy: LaunchStrategy = LaunchStrategy.NewTask()) {
        val normalizedTargetDeeplink = deeplink.normalized
        val direction = directions.firstOrNull { direction ->
            direction.deeplinks.any { it.normalized == normalizedTargetDeeplink }
        } ?: throw IllegalArgumentException("No direction found for deeplink: $deeplink")

        val route = deeplink.queryParamsAsRoute(direction.routeClass)
            ?: throw IllegalArgumentException(
                "Invalid deeplink for route: ${direction.routeClass.qualifiedName}." +
                        " Deeplink: $deeplink"
            )

        navigateTo(route = route, strategy = strategy)
    }

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
    ): Boolean = runCatching {
        navigateTo(deeplink, strategy)
        true
    }.getOrElse {
        Lumber.tag("NavigationController").error(it)
        false
    }

    /**
     * Pops the back stack up to a given destination route.
     *
     * This will remove all destinations from the top of the stack down to the specified [direction].
     *
     * @param direction The destination [NavigationRoute] to pop up to.
     * @param inclusive If `true`, the destination itself will also be popped from the stack.
     * @throws IllegalStateException if `inclusive` is true and the target `direction` is the root of the back stack.
     */
    @Throws(IllegalStateException::class)
    fun popUpTo(direction: NavigationRoute, inclusive: Boolean = false) {
        if (direction !in backStack) return

        val directionIndex = backStack.indexOfLast { it::class == direction::class }
        if (directionIndex == 0 && inclusive) {
            // TODO Handle 2 backStacks and stop displaying navigation when root popped
            throw IllegalStateException("Cannot pop root destination")
        }

        val startIndex = if (inclusive) directionIndex else directionIndex + 1
        runCatching { backStack.removeRange(startIndex, backStack.size) }
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
    fun safePopUpTo(direction: NavigationRoute, inclusive: Boolean = false): Boolean = runCatching {
        popUpTo(direction, inclusive)
        true
    }.getOrElse {
        Lumber.tag("NavigationController").error(it)
        false
    }

    @OptIn(InternalSerializationApi::class)
    private fun String.queryParamsAsRoute(routeClass: KClass<out NavigationRoute>): NavigationRoute? {
        val map = if (matches(Regex("^[^?]+\\?[^=]+=.+$"))) {
            split("?").last().split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }
        } else {
            emptyMap()
        }

        return Json.runCatching {
            decodeFromString(routeClass.serializer(), encodeToString(map))
        }.getOrNull()
    }

    companion object {
        /**
         * Creates and remembers a [NavigationController] instance.
         *
         * @param initialRoute The initial route to be displayed when the navigation is first set up.
         * @param directionRegistries A list of [DirectionRegistry] instances containing all possible navigation directions.
         * @param backStack An optional [SnapshotStateList] to be used as the back stack. If not provided,
         * a new one will be created with the [initialRoute].
         * @return A remembered [NavigationController] instance.
         */
        @Composable
        operator fun invoke(
            initialRoute: NavigationRoute,
            directionRegistries: List<DirectionRegistry>,
            backStack: SnapshotStateList<NavigationRoute> = remember {
                mutableStateListOf(initialRoute)
            },
        ) = NavigationController(backStack = backStack, directionRegistryList = directionRegistries)
    }
}
