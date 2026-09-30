package com.pedrobneto.easy.navigation.core.modal

/**
 * Marks a navigation destination to be displayed above the current navigation scene.
 *
 * The destination remains in the navigation back stack. Its content owns its visual presentation
 * and dismissal behavior.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Modal
