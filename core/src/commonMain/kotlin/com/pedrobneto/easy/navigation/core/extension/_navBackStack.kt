package com.pedrobneto.easy.navigation.core.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import com.pedrobneto.easy.navigation.core.defaultNavigationJson
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Creates and remembers a [NavBackStack] that is automatically saved and restored across process deaths.
 *
 * This function uses [rememberSerializable] to handle the saving and restoration of the navigation back stack.
 * It configures polymorphic serialization for the [NavigationRoute] sealed class, allowing for different
 * route types to be correctly serialized and deserialized.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and recomposition does not reset the stack when initialRoute changes.
 *
 * @param initialRoute The initial [NavigationRoute] to be placed on a new back stack.
 * @param registries A list of [DirectionRegistry] instances. These registries provide the serialization
 * mappings for all concrete subtypes of [NavigationRoute], which is essential for the polymorphic
 * serialization to work correctly.
 * @return A remembered [NavBackStack] instance that is state-saved.
 */
@Composable
fun rememberNavBackStack(
    initialRoute: NavigationRoute,
    registries: List<DirectionRegistry>
): NavBackStack<NavigationRoute> = rememberNavigationBackStack(registries) { initialRoute }

/**
 * Creates a saveable stack from a deeplink using the same resolution as controller navigation.
 * Uses [rememberSerializable] to save and restore the stack with the route serializers from registries.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 * Deeplink resolution errors propagate to the caller.
 *
 * @param initialRoute The initial deeplink resolved against the destinations in registries for a new back stack.
 * @param registries The registries that provide deeplink destinations and serializers for all concrete [NavigationRoute] subtypes.
 * @param json The JSON configuration used to deserialize deeplink arguments. Back stack saving and restoration use the serializers supplied by registries.
 * @return A remembered [NavBackStack] that is automatically saved and restored.
 */
@Composable
fun rememberNavBackStack(
    initialRoute: String,
    registries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavBackStack<NavigationRoute> = rememberNavigationBackStack(registries) {
    NavigationDeeplink(initialRoute).resolve(json, registries.flatMap(DirectionRegistry::directions))
}

/**
 * Creates a saveable stack from a deeplink and serializable payload.
 * Uses [rememberSerializable] to save and restore the stack with the route serializers from registries.
 *
 * Initial arguments only apply when creating a new stack. Restored state takes precedence,
 * and changes to initial arguments during recomposition do not reset or navigate the stack.
 * Deeplink resolution errors propagate to the caller.
 *
 * @param initialRoute The initial deeplink resolved against the destinations in registries for a new back stack.
 * @param payload The serializable payload for the initial deeplink. It must encode to a JSON object; its arguments override query and path arguments.
 * @param registries The registries that provide deeplink destinations and serializers for all concrete [NavigationRoute] subtypes.
 * @param json The JSON configuration used to serialize the payload and deserialize deeplink arguments. Back stack saving and restoration use the serializers supplied by registries.
 * @return A remembered [NavBackStack] that is automatically saved and restored.
 */
@Composable
inline fun <reified T> rememberNavBackStack(
    initialRoute: String,
    payload: T,
    registries: List<DirectionRegistry>,
    json: Json = defaultNavigationJson(),
): NavBackStack<NavigationRoute> = rememberNavigationBackStack(registries) {
    NavigationDeeplink(initialRoute).resolve(json, registries.flatMap(DirectionRegistry::directions), payload)
}

@PublishedApi
@Composable
internal fun rememberNavigationBackStack(
    registries: List<DirectionRegistry>,
    initialRoute: () -> NavigationRoute,
): NavBackStack<NavigationRoute> = rememberSerializable(
    configuration = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavigationRoute::class) {
                registries.forEach { it.registerAll(this) }
            }
        }
    },
    serializer = NavBackStackSerializer(PolymorphicSerializer(NavigationRoute::class)),
    init = { NavBackStack(initialRoute()) }
)

/**
 * Removes a range of entries from the NavBackStack.
 *
 * This method removes elements from this list starting at [fromIndex] (inclusive) and up to [toIndex] (exclusive).
 *
 * @param fromIndex The index of the first element to be removed.
 * @param toIndex The index of the last element to be removed.
 */
fun NavBackStack<*>.removeRange(fromIndex: Int, toIndex: Int) =
    subList(fromIndex, toIndex).clear()
