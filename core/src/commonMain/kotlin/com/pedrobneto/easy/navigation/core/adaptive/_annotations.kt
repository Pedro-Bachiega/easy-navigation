package com.pedrobneto.easy.navigation.core.adaptive

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

/**
 * Marks a navigation destination to be displayed in an adaptive pane.
 *
 * An adaptive pane can host an extra pane alongside it, creating a two-pane layout.
 * This is the default behavior for all destinations if no other pane annotation is specified.
 *
 * @property ratio The fraction of the available width that this pane should occupy when displayed alone.
 * Defaults to `1f`, meaning it takes up the full width. This property is used when the adaptive pane
 * is shown without an extra pane.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class AdaptivePane(val ratio: Float = 1f)

/**
 * Marks a navigation destination to be displayed as an extra pane, typically alongside a primary pane.
 *
 * This is used to create a two-pane layout, where this destination appears next to its `host`.
 * If this destination is navigated to from a route other than its specified `host`, it will be
 * displayed as a single pane instead.
 *
 * @property host The [KClass] of the [NavigationRoute] that acts as the host for this extra pane.
 * @property ratio The fraction of the available width that this extra pane should occupy.
 * The host pane will occupy the remaining `1f - ratio`. Defaults to `0.5f`.
 */
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExtraPane(val host: KClass<out NavigationRoute>, val ratio: Float = 0.5f)


/**
 * Marks a navigation destination to be displayed as a single pane.
 *
 * A single pane will always occupy the entire available space and will not allow any extra panes
 * to be displayed alongside it. When a destination marked as `SinglePane` is on top of the
 * navigation stack, any previous two-pane layouts will be replaced by this single pane.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SinglePane
