package com.pedrobneto.navigation.core.model

/**
 * A registry of navigation directions.
 *
 * This class is responsible for holding a list of all possible [NavigationDirection]s in a given module.
 *
 * @property directions The list of navigation directions.
 */
abstract class DirectionRegistry(
    val directions: List<NavigationDirection>
)
