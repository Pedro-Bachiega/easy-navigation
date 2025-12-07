package com.pedrobneto.easy.navigation.core.adaptive

import androidx.annotation.FloatRange
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
    data class Adaptive(
        @param:FloatRange(from = .1, to = 1.0)
        val ratio: Float = 1f
    ) : PaneStrategy()

    /**
     * The content will always be displayed in a single pane, regardless of the `NavHost` configuration.
     */
    @Serializable
    data object Single : PaneStrategy()

    /**
     * The content will be displayed in an extra pane, alongside the primary one.
     *
     * @property hosts The hosts this pane can be attached to with their corresponding ratio.
     */
    @Serializable
    data class Extra(val hosts: List<PaneHost>) : PaneStrategy() {
        constructor(vararg hosts: PaneHost) : this(hosts = hosts.toList())

        /**
         * The host this pane can be attached to.
         *
         * @property route The `route` this pane can be attached to.
         * @property ratio The ratio of the screen that the extra pane will occupy.
         */
        @Serializable
        data class PaneHost(
            val route: KClass<out NavigationRoute>,
            @param:FloatRange(from = .1, to = .9)
            val ratio: Float = 0.5f
        )
    }
}
