package com.pedrobneto.easy.navigation.core.model

import androidx.navigation3.runtime.NavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * A registry of navigation directions.
 *
 * This class is responsible for holding a list of all possible [NavigationDirection]s in a given module.
 *
 * @property directions The list of navigation directions.
 */
abstract class DirectionRegistry(val directions: List<NavigationDirection>) {
    /**
     * Registers every direction into the `serializersModule` of the [NavBackStack]'s [SavedStateConfiguration]
     */
    fun registerAll(builder: PolymorphicModuleBuilder<NavigationRoute>) =
        directions.forEach { it.register(builder) }
}
