package com.pedrobneto.navigation.annotation

import kotlin.reflect.KClass

/**
 * Marks a composable function as a navigation entry.
 *
 * This annotation is used by the navigation processor to generate navigation code.
 *
 * @property route The route associated with this navigation entry.
 * @property deeplinks An array of deeplink URIs that lead to this navigation entry.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class NavigationEntry(val route: KClass<*>, val deeplinks: Array<String> = [])
