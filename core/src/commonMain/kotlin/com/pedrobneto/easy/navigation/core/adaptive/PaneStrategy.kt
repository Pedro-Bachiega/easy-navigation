package com.pedrobneto.easy.navigation.core.adaptive

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Defines the pane strategy for a navigation destination.
 */
@Serializable
sealed class PaneStrategy {
    /**
     * The content will be displayed in a single pane, or in an adaptive way,
     * following the configuration set in the `NavHost`.
     */
    @Serializable
    data class Adaptive(val ratio: Float = 1f) : PaneStrategy()

    /**
     * The content will always be displayed in a single pane, regardless of the `NavHost` configuration.
     */
    @Serializable
    data object Single : PaneStrategy()

    /**
     * The content will be displayed in an extra pane, alongside the primary one.
     *
     * @property host The host navigation route that will display the extra pane.
     * @property ratio The ratio of the screen that the extra pane will occupy.
     */
    @Serializable
    data class Extra(val host: KClass<out NavigationRoute>, val ratio: Float = 0.5f) : PaneStrategy()
}
