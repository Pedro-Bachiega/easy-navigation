package com.pedrobneto.easy.navigation.core.model

/** The outcome delivered to a navigation result launcher. */
sealed interface NavigationResult<out T> {
    /** The destination returned [value]. */
    data class Confirmed<T>(val value: T) : NavigationResult<T>

    /** The destination was removed without returning a value. */
    data object Cancelled : NavigationResult<Nothing>
}
