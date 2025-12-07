package com.pedrobneto.easy.navigation.core.model

import androidx.compose.runtime.mutableStateListOf
import com.pedrobneto.easy.navigation.core.NavigationController
import com.pedrobneto.easy.navigation.core.extension.removeRange

/**
 * Defines how a navigation destination should be launched, affecting the back stack.
 * This is analogous to Android's activity launch modes.
 */
sealed class LaunchStrategy {
    /**
     * Handles the navigation to a given [route] based on the specific strategy,
     * modifying the [controller]'s back stack.
     */
    internal abstract fun handleNavigation(route: NavigationRoute, controller: NavigationController)

    /**
     * The default navigation behavior. A new instance of the destination is always pushed onto the
     * back stack.
     */
    data object Default : LaunchStrategy() {
        override fun handleNavigation(route: NavigationRoute, controller: NavigationController) {
            controller.backStack.add(route)
        }
    }

    /**
     * The `SingleTop` launch strategy affects how a destination is handled when it's already
     * present in the back stack.
     *
     * If there's a single instance of the destination and it is already at the top of the stack, it's replaced by the
     * new instance. This is useful for updating the current screen with new arguments without
     * pushing a new copy onto the stack.
     *
     * @property clearTop If `true`, and an instance of the destination exists in the back stack but not at the top,
     * the stack is popped until the first instance of that destination is reached. This first instance
     * and all destinations above it are removed, and the new destination is pushed onto the stack.
     * If `false`, and instances of the destination exist in the back stack, all of them are removed,
     * and the new destination is pushed to the top of the stack.
     */
    class SingleTop(val clearTop: Boolean = true) : LaunchStrategy() {
        override fun handleNavigation(route: NavigationRoute, controller: NavigationController) {
            val backStack = controller.backStack
            val existingInstances = backStack.filter { it::class == route::class }
            val isAlreadyOnTop = backStack.lastOrNull()?.let { it::class == route::class } == true

            when {
                existingInstances.size == 1 && isAlreadyOnTop -> {
                    backStack[backStack.lastIndex] = route
                    return
                }

                existingInstances.isNotEmpty() && clearTop -> {
                    val indexOfFirst = backStack.indexOf(existingInstances.first())
                    backStack.removeRange(indexOfFirst, backStack.size)
                }

                existingInstances.isNotEmpty() -> {
                    backStack.removeAll(existingInstances)
                }
            }

            backStack.add(route)
        }
    }

    /**
     * The entire back stack is cleared and the new destination becomes the new root.
     * This is useful for flows that should not allow returning to the previous flow, such as after a login or logout.
     */
    data object NewStack : LaunchStrategy() {
        override fun handleNavigation(route: NavigationRoute, controller: NavigationController) {
            controller.backStack.add(route)
            controller.backStack.removeRange(0, controller.backStack.lastIndex)
        }
    }
}
