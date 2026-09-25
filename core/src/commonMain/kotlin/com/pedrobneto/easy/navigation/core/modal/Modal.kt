package com.pedrobneto.easy.navigation.core.modal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.pedrobneto.easy.navigation.core.transition.ModalTransitions
import kotlin.reflect.KClass

/**
 * Marks a navigation destination to be displayed above the current navigation scene.
 *
 * The destination remains in the navigation back stack. When [dismissible] is `true`, the
 * system back action and taps outside the modal content dismiss it. Explicit calls to
 * `NavigationController.navigateUp` are always allowed.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Modal(
    val dismissible: Boolean = true,
    val transitions: KClass<out ModalTransitions> = InheritModalTransitions::class,
)

/** Marker used by [Modal] when the navigation-level modal transitions should be used. */
class InheritModalTransitions : ModalTransitions {
    override val contentEnter: EnterTransition = EnterTransition.None
    override val contentExit: ExitTransition = ExitTransition.None
    override val scrimEnter: EnterTransition = EnterTransition.None
    override val scrimExit: ExitTransition = ExitTransition.None
}

/** Configuration carried by a generated modal navigation direction. */
data class ModalConfig(
    val dismissible: Boolean = true,
    val transitions: ModalTransitions? = null,
)
