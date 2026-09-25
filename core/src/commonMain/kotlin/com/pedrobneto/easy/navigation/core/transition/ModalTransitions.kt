package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

/** Animations used when a modal overlay enters or leaves the current scene. */
interface ModalTransitions {
    val contentEnter: EnterTransition
    val contentExit: ExitTransition
    val scrimEnter: EnterTransition
    val scrimExit: ExitTransition
}
