package com.pedrobneto.easy.navigation.core.annotation

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

/**
 * Associates a deeplink URI with a navigation destination.
 *
 * This annotation allows a composable function to be invoked when a specific URI is navigated to.
 * It can be repeated to associate multiple deeplinks with the same destination.
 *
 * @property value The deeplink URI, which can include placeholders for arguments (e.g., "app://user/{userId}").
 */
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Deeplink(val value: String)

/**
 * Specifies a deeplink to navigate to when `navigateUp` is called from the root of a navigation graph.
 *
 * When a destination is the initial route in its navigation graph, calling `navigateUp` would typically
 * have no effect within that graph. By annotating a destination with [ParentDeeplink], you define an
 * explicit upward navigation target.
 *
 * @property value The deeplink of the parent [NavigationRoute] to navigate to.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ParentDeeplink(val value: String)

/**
 * Designates a composable function as a destination for a specific [NavigationRoute].
 *
 * The annotation processor uses this to link a route class to its corresponding UI content.
 *
 * @property value The [KClass] of the [NavigationRoute] that this destination represents.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Route(val value: KClass<out NavigationRoute>)

/**
 * Specifies a route to navigate to when `navigateUp` is called from the root of a navigation graph.
 *
 * When a destination is the initial route in its navigation graph, calling `navigateUp` would typically
 * have no effect within that graph. By annotating a destination with [ParentRoute], you define an
 * explicit upward navigation target.
 *
 * @property value The [KClass] of the parent [NavigationRoute] to navigate to.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ParentRoute(val value: KClass<out NavigationRoute>)

/**
 * Assigns a scope to a navigation destination.
 *
 * Scopes can be used by the annotation processor to generate code that groups related
 * navigation components, such as closed navigation graphs.
 *
 * @property value A unique identifier for the scope.
 */
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Scope(val value: String)

/**
 * Marks the route as a direction to be registered in the `GlobalDirectionRegistry`.
 */
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class GlobalScope
