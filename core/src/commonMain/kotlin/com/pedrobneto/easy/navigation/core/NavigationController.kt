@file:OptIn(InternalSerializationApi::class)

package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
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
import com.pedrobneto.easy.navigation.core.model.NavigationResult
import com.pedrobneto.easy.navigation.core.modal.ModalScope
import com.pedrobneto.easy.navigation.test.KoverExcludes
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * A CompositionLocal that provides access to the [NavigationController] instance.
 * This should be used to access the navigation controller from within a composable.
 */
@KoverExcludes
val LocalNavigationController: ProvidableCompositionLocal<NavigationController> =
    staticCompositionLocalOf { error("Navigation not initialized. Make sure you have a Navigation composable in your hierarchy.") }

@KoverExcludes
internal val LocalParentNavigationController: ProvidableCompositionLocal<NavigationController?> =
    staticCompositionLocalOf { null }

/**
 * Remembers a controller initialized from a route.
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param initialRoute The initial [NavigationRoute] placed on a new back stack.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @return The remembered [NavigationController] for the navigation back stack.
 */
@Composable
@KoverExcludes
fun rememberNavigationController(
    initialRoute: NavigationRoute,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavigationController = rememberNavigationController(
    backStack = rememberNavBackStack(
        initialRoute = initialRoute,
        registries = directionRegistries,
    ),
    directionRegistries = directionRegistries,
    json = json,
)

/**
 * Remembers a controller initialized from a deeplink.
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param initialRoute The initial deeplink resolved against directionRegistries for a new back stack. Resolution errors propagate to the caller.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @return The remembered [NavigationController] for the navigation back stack.
 */
@Composable
@KoverExcludes
fun rememberNavigationController(
    initialRoute: String,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavigationController = rememberNavigationController(
    backStack = rememberNavBackStack(
        initialRoute = initialRoute,
        registries = directionRegistries,
        json = json,
    ),
    directionRegistries = directionRegistries,
    json = json,
)

/**
 * Remembers a controller initialized from a deeplink and serializable payload.
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 *
 * @param initialRoute The initial deeplink resolved against directionRegistries for a new back stack. Resolution errors propagate to the caller.
 * @param payload The serializable payload for the initial deeplink. It must encode to a JSON object; its arguments override query and path arguments.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @return The remembered [NavigationController] for the navigation back stack.
 */
@Composable
@KoverExcludes
inline fun <reified T> rememberNavigationController(
    initialRoute: String,
    payload: T,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavigationController = rememberNavigationController(
    backStack = rememberNavBackStack(
        initialRoute = initialRoute,
        payload = payload,
        registries = directionRegistries,
        json = json,
    ),
    directionRegistries = directionRegistries,
    json = json,
)

/**
 * Remembers a controller for an existing back stack without requiring an initial route.
 * The caller owns the stack's saving and restoration.
 *
 * @param backStack The existing navigation back stack used directly by the controller. The caller manages its saving and restoration.
 * @param directionRegistries The registries that provide navigation destinations and route serializers.
 * @param json The JSON configuration used to resolve deeplink arguments and serialize navigation payloads and results. The controller retains this instance for later navigation.
 * @return The remembered [NavigationController] for the navigation back stack.
 */
@Composable
@KoverExcludes
fun rememberNavigationController(
    backStack: NavBackStack<NavigationRoute>,
    directionRegistries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavigationController {
    val parentController = LocalParentNavigationController.current
    val resultState = rememberSerializable(
        stateSerializer = NavigationResultState.serializer(),
        init = { mutableStateOf(NavigationResultState.initial(backStack.size)) },
    )
    return remember(backStack, directionRegistries, parentController, json, resultState) {
        NavigationController(
            backStack = backStack,
            directionRegistryList = directionRegistries,
            parentController = parentController,
            json = json,
            navigationResultState = resultState,
        )
    }
}

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
    @PublishedApi internal val json: Json,
    private val parentController: NavigationController? = null,
    private val parentRoute: NavigationRoute? = parentController?.currentRoute,
    internal val navigationResultState: androidx.compose.runtime.MutableState<NavigationResultState> =
        mutableStateOf(NavigationResultState.initial(backStack.size)),
) {
    private val modalScopes = mutableListOf<ModalScope>()
    private val resultCallbacks = mutableMapOf<Long, (NavigationResultCompletion) -> Unit>()
    private var navigationMutationDepth = 0
    private val deferredResultDispatches = mutableSetOf<Long>()

    @PublishedApi
    internal val directions: List<NavigationDirection> =
        directionRegistryList.flatMap(DirectionRegistry::directions)

    internal val currentDirection: NavigationDirection
        get() = directions.find { it.routeClass == currentRoute::class }
            ?: error("No direction found for route $currentRoute")

    internal fun handleSystemBack() {
        if (currentDirection.isModal) {
            val modalScope = modalScopes.lastOrNull { it.route == currentRoute }
            if (modalScope?.dispatchSystemBackRequest() == true) return
        }
        safeNavigateUp()
    }

    internal fun registerModalScope(modalScope: ModalScope) {
        modalScopes.remove(modalScope)
        modalScopes.add(modalScope)
    }

    internal fun unregisterModalScope(modalScope: ModalScope) {
        modalScopes.remove(modalScope)
    }

    /**
     * Provides a [NavEntry] for a given [NavigationRoute], allowing the navigation framework
     * to render the correct composable for each route.
     */
    internal val directionProvider: (NavigationRoute) -> NavEntry<NavigationRoute> = entryProvider {
        directions.forEach { direction ->
            addEntryProvider(
                clazz = direction.routeClass,
                metadata = direction.metadata,
                content = direction::Draw
            )
        }
    }

    /**
     * Indicates whether we have a parent route to navigate to or not.
     *
     * `true` if [currentIndex] is not `0`.
     */
    val canNavigateUp: Boolean get() = currentIndex > 0

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
        withNavigationMutation { strategy.handleNavigation(route = route, controller = this) }

    /**
     * Confirms the result for the current destination and navigates up.
     *
     * The result is stored before navigating up and delivered to the launcher after the
     * navigation attempt. If there is no destination to return to, the result is still delivered
     * and this method throws the same exception as [navigateUp].
     */
    @UnsafeNavigationApi
    inline fun <reified T> navigateUpWithResult(value: T) {
        navigateUpWithSerializedResult(json.encodeToString(serializer<T>(), value))
    }

    /**
     * Confirms the result for the current destination and attempts to navigate up.
     *
     * The result is delivered even when navigating up fails; in that case this method returns
     * `false`.
     */
    @SafeNavigationApi
    inline fun <reified T> safeNavigateUpWithResult(value: T): Boolean =
        runCatching { navigateUpWithResult(value) }.isSuccess

    @PublishedApi
    internal fun navigateUpWithSerializedResult(valueJson: String) {
        val launcherId = completeResult(currentResultEntry().id, valueJson, cancelled = false)
        val navigationAttempt = runCatching { navigateUp() }
        launcherId?.let(::dispatchCompletion)
        navigationAttempt.getOrThrow()
    }

    @PublishedApi
    internal fun allocateResultLauncherId(): Long {
        val current = navigationResultState.value
        val id = current.nextLauncherId
        navigationResultState.value = current.copy(nextLauncherId = id + 1)
        return id
    }

    @PublishedApi
    internal fun <T> registerResultCallback(
        launcherId: Long,
        serializer: KSerializer<T>,
        callback: (NavigationResult<T>) -> Unit,
    ) {
        resultCallbacks[launcherId] = { completion ->
            if (completion.cancelled) {
                callback(NavigationResult.Cancelled)
            } else {
                val jsonValue = requireNotNull(completion.valueJson)
                callback(NavigationResult.Confirmed(json.decodeFromString(serializer, jsonValue)))
            }
        }
        dispatchCompletion(launcherId)
    }

    @PublishedApi
    internal fun unregisterResultCallback(launcherId: Long) {
        resultCallbacks.remove(launcherId)
    }

    @PublishedApi
    internal fun isResultLauncherPending(launcherId: Long): Boolean =
        alignedResultState().entries.any { it.request?.launcherId == launcherId } ||
            navigationResultState.value.completions.any { it.launcherId == launcherId }

    @PublishedApi
    internal fun navigateForResult(
        route: NavigationRoute,
        strategy: LaunchStrategy,
        launcherId: Long,
    ): Boolean {
        if (isResultLauncherPending(launcherId)) return false
        withNavigationMutation {
            strategy.handleNavigation(route, this, launcherId)
        }
        return true
    }

    private inline fun <T> withNavigationMutation(block: () -> T): T {
        navigationMutationDepth++
        return try {
            block()
        } finally {
            navigationMutationDepth--
            if (navigationMutationDepth == 0) {
                deferredResultDispatches.toList().forEach(::dispatchCompletion)
                deferredResultDispatches.clear()
            }
        }
    }

    internal fun addRoute(
        route: NavigationRoute,
        launcherId: Long? = null,
    ) {
        val state = alignedResultState()
        backStack.add(route)
        val request = launcherId?.let(::NavigationResultRequest)
        navigationResultState.value = state.copy(
            entries = state.entries + NavigationResultStackEntry(state.nextEntryId, request),
            nextEntryId = state.nextEntryId + 1,
        )
    }

    internal fun replaceRoute(
        index: Int,
        route: NavigationRoute,
        launcherId: Long? = null,
    ) {
        val state = alignedResultState()
        val oldEntry = state.entries[index]
        backStack[index] = route
        val replacement = NavigationResultStackEntry(
            id = state.nextEntryId,
            request = launcherId?.let(::NavigationResultRequest),
        )
        val entries = state.entries.toMutableList().also { it[index] = replacement }
        navigationResultState.value = state.copy(entries = entries, nextEntryId = state.nextEntryId + 1)
        oldEntry.request?.let { completeResultForRequest(it, cancelled = true) }
    }

    internal fun removeRoutes(fromIndex: Int, toIndex: Int) {
        if (fromIndex >= toIndex) return
        val state = alignedResultState()
        val removed = state.entries.subList(fromIndex, toIndex)
        val requests = removed.mapNotNull { it.request }
        backStack.removeRange(fromIndex, toIndex)
        navigationResultState.value = state.copy(
            entries = state.entries.take(fromIndex) + state.entries.drop(toIndex),
        )
        requests.forEach { completeResultForRequest(it, cancelled = true) }
    }

    internal fun removeRouteIndices(indices: List<Int>) {
        indices.sortedDescending().forEach { removeRoutes(it, it + 1) }
    }

    internal fun replaceStackWith(
        route: NavigationRoute,
        launcherId: Long? = null,
    ) {
        removeRoutes(0, backStack.size)
        addRoute(route, launcherId)
    }

    private fun alignedResultState(): NavigationResultState {
        val current = navigationResultState.value
        if (current.entries.size == backStack.size) return current

        val cancelledRequests = current.entries.mapNotNull { it.request }
        val reset = current.copy(
            entries = List(backStack.size) { index ->
                NavigationResultStackEntry(current.nextEntryId + index)
            },
            nextEntryId = current.nextEntryId + backStack.size,
        )
        navigationResultState.value = reset
        cancelledRequests.forEach { completeResultForRequest(it, cancelled = true) }
        return navigationResultState.value
    }

    private fun currentResultEntry(): NavigationResultStackEntry = alignedResultState().entries.last()

    private fun completeResult(entryId: Long, valueJson: String?, cancelled: Boolean): Long? {
        val state = alignedResultState()
        val entryIndex = state.entries.indexOfFirst { it.id == entryId }
        if (entryIndex < 0) return null
        val request = state.entries[entryIndex].request ?: return null
        val entries = state.entries.toMutableList().also {
            it[entryIndex] = it[entryIndex].copy(request = null)
        }
        navigationResultState.value = state.copy(
            entries = entries,
            completions = state.completions.filterNot { it.launcherId == request.launcherId } +
                NavigationResultCompletion(request.launcherId, valueJson, cancelled),
        )
        return request.launcherId
    }

    private fun completeResultForRequest(request: NavigationResultRequest, cancelled: Boolean) {
        val state = navigationResultState.value
        val exists = state.completions.any { it.launcherId == request.launcherId }
        if (!exists) {
            navigationResultState.value = state.copy(
                completions = state.completions + NavigationResultCompletion(
                    launcherId = request.launcherId,
                    cancelled = cancelled,
                ),
            )
        }
        dispatchCompletion(request.launcherId)
    }

    private fun dispatchCompletion(launcherId: Long) {
        if (navigationMutationDepth > 0) {
            deferredResultDispatches += launcherId
            return
        }
        val callback = resultCallbacks[launcherId] ?: return
        val completion = navigationResultState.value.completions.firstOrNull {
            it.launcherId == launcherId
        } ?: return
        navigationResultState.value = navigationResultState.value.copy(
            completions = navigationResultState.value.completions.filterNot {
                it.launcherId == launcherId
            },
        )
        callback(completion)
    }

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
     * @throws IllegalArgumentException if no direction is found for the deeplink or if the
     * deeplink is malformed for the target route.
     */
    @UnsafeNavigationApi
    @Throws(IllegalArgumentException::class)
    inline fun <reified T> navigateTo(
        deeplink: String,
        payload: T,
        strategy: LaunchStrategy = LaunchStrategy.Default
    ) = navigateTo(
        route = NavigationDeeplink(deeplink).resolve(json, directions, payload),
        strategy = strategy
    )

    /**
     * Navigates to a destination via a deeplink URI and payload.
     *
     * @param deeplink The deeplink URI to navigate to.
     * @param payload Additional route arguments to merge with path and query parameters.
     * @param strategy The [LaunchStrategy] to apply to this navigation action.
     * @return `true` if the navigation was successful, `false` otherwise.
     */
    @SafeNavigationApi
    inline fun <reified T> safeNavigateTo(
        deeplink: String,
        payload: T,
        strategy: LaunchStrategy = LaunchStrategy.Default
    ): Boolean = runCatching { navigateTo(deeplink, payload, strategy) }.isSuccess

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
                if (parentController != null && !canNavigateUp) {
                    val routeToRestore = parentRoute
                    val parentRouteIndex = routeToRestore?.let {
                        parentController.backStack.indexOfLast { route -> route == it }
                    } ?: -1

                    if (routeToRestore != null &&
                        parentRouteIndex >= 0 &&
                        parentController.currentRoute != routeToRestore
                    ) {
                        parentController.popUpTo(routeToRestore)
                        return
                    }

                    if (parentController.canNavigateUp) {
                        parentController.navigateUp()
                        return
                    }
                }

                val message = "Not a nested navigation. Cannot pop root destination."
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

                        route != null -> replaceRoute(currentIndex, route)
                    }
                }.getOrElse { exception ->
                    val message = "Could not decode route for parent."
                    Lumber.tag("NavigationController").error(exception, message)
                    throw IllegalArgumentException(message, exception)
                }
            }

            else -> {
                val startIndex = if (inclusive) targetRouteIndex else targetRouteIndex + 1
                removeRoutes(startIndex, backStack.size)
            }
        }
    }
}
