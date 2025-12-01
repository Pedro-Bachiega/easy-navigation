package com.pedrobneto.navigation.core.model

import com.pedrobneto.navigation.core.NavigationController

/**
 * Defines how a navigation destination should be launched, affecting the back stack.
 * This is analogous to Android's activity launch modes.
 */
sealed class LaunchStrategy {
    internal abstract fun handleNavigation(route: NavigationRoute, controller: NavigationController)

    /**
     * The `SingleTop` launch strategy affects how a destination is handled when it's already
     * in the back stack.
     *
     * If the destination is already at the top of the stack, it will be replaced with the new instance,
     * useful for updating route arguments without changing the screen.
     *
     * @property clearTop If `true`, and an instance of the destination exists in the back stack but not at the top,
     * all destinations above it will be popped, bringing the existing instance to the top.
     * If `false`, and an instance of the destination exists in the back stack but not at the top,
     * the existing destination will be cleared and a new instance will be pushed on top of the stack.
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
     * A launch strategy that can clear the back stack before launching the new destination.
     *
     * @property clearStack If `true`, the entire back stack is cleared and the new destination becomes the new root.
     * This is useful for flows that should not allow returning to the previous flow, such as after a login or logout.
     * If `false`, a new destination is simply pushed on top of the current back stack.
     */
    class NewTask(val clearStack: Boolean = false) : LaunchStrategy() {
        override fun handleNavigation(route: NavigationRoute, controller: NavigationController) {
            controller.backStack.add(route)
            if (clearStack) controller.backStack.removeRange(0, controller.backStack.lastIndex)
        }
    }
}
