package com.pedrobneto.easy.navigation.core.annotation

/**
 * A DSL marker for Easy Navigation functions.
 *
 * This annotation indicates that a function is "safe," meaning it is not expected to throw
 * an exception under normal-use conditions. Functions marked with this annotation have safeguards,
 * often through compile-time checks, to prevent runtime errors related to navigation.
 */
@DslMarker
annotation class SafeNavigationApi

/**
 * A DSL marker for Easy Navigation functions.
 *
 * This annotation indicates that a function is "unsafe," meaning it may throw an exception
 * at runtime. This can happen, for example, when navigating to a route that is not
 * registered or providing incorrect arguments. Callers of functions marked with this
 * annotation should be prepared to handle potential exceptions.
 */
@DslMarker
annotation class UnsafeNavigationApi
