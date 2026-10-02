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
 * @param initialRoute The initial [NavigationRoute] to be placed on the back stack.
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
 * The deeplink is only resolved for a new stack; restored state takes precedence.
 * Resolution errors propagate to the caller.
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
 * Payload arguments have the same precedence as in NavigationController.navigateTo.
 * The deeplink and payload are only resolved when creating a new stack.
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
