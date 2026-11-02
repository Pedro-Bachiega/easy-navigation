package com.pedrobneto.easy.navigation.core.transition

import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/** Transient intent attached to one complete controller mutation; never saved or serialized. */
internal class NavigationTransitionChange(
    val before: List<NavigationRoute>,
    val after: List<NavigationRoute>,
    val operation: NavigationOperation,
)

internal val LaunchStrategy.navigationOperation: NavigationOperation
    get() = when (this) {
        LaunchStrategy.Default -> NavigationOperation.Forward
        is LaunchStrategy.SingleTop -> NavigationOperation.SingleTop
        LaunchStrategy.NewStack -> NavigationOperation.NewStack
    }
