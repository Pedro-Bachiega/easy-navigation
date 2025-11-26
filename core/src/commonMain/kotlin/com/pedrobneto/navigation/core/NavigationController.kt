package com.pedrobneto.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Composition local for accessing the [NavigationController] instance.
 */
val LocalNavigationController: ProvidableCompositionLocal<NavigationController> =
    staticCompositionLocalOf { error("Navigation not initialized") }

/**
 * Handles navigation within the application.
 *
 * @property backStack The current navigation back stack.
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
     * Provides a [NavEntry] for a given [NavigationRoute].
     */
    internal val directionProvider: (NavigationRoute) -> NavEntry<NavigationRoute> =
        entryProvider {
            directions.map { direction ->
                addEntryProvider(clazz = direction.routeClass, content = direction::Draw)
            }
        }

    /**
     * Navigates up in the back stack.
     *
     * @return `true` if the navigation was successful, `false` otherwise.
     */
    fun navigateUp() = backStack.removeLastOrNull() != null

    /**
     * Navigates to a given [route].
     *
     * @param route The destination to navigate to.
     * @param singleTop If `true`, and the destination is already on the back stack, the back stack will be popped up to that destination.
     */
    fun navigateTo(route: NavigationRoute, singleTop: Boolean = false) {
        if (singleTop && route in backStack) popUpTo(route, true)
        backStack.add(route)
    }

    /**
     * Navigates to a given [deeplink].
     *
     * @param deeplink The deeplink to navigate to.
     * @param singleTop If `true`, and the destination is already on the back stack, the back stack will be popped up to that destination.
     */
    fun navigateTo(deeplink: String, singleTop: Boolean = false) {
        val normalizedTargetDeeplink = deeplink.normalized
        directions.firstOrNull { direction ->
            direction.deeplinks.any { it.normalized == normalizedTargetDeeplink }
        }?.let { direction ->
            val route = deeplink.queryParamsAsRoute(direction.routeClass) ?: return
            navigateTo(route, singleTop)
        }
    }

    /**
     * Pops the back stack up to a given [direction].
     *
     * @param direction The destination to pop up to.
     * @param inclusive If `true`, the destination itself will be popped.
     */
    fun popUpTo(direction: NavigationRoute, inclusive: Boolean = false) {
        if (direction !in backStack) return

        val directionIndex = backStack.indexOfFirst { it == direction }
        val startIndex = if (inclusive) directionIndex else directionIndex + 1
        runCatching { backStack.removeRange(startIndex, backStack.size) }
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
         * Creates a new [NavigationController] instance.
         *
         * @param initialRoute The initial route.
         * @param directionRegistries The direction registries to use.
         * @param backStack The initial back stack.
         * @return A new [NavigationController] instance.
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
